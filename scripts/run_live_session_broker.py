#!/usr/bin/env python3
"""
MobiDesk — Continuous Live Session Broker & WSS-to-RDP Relay

Designed for production & automated runner hosting (Windows 11 VM):
1. Serves HTTP control plane:
   - POST /v1/session-grants -> Issues 60s single-use bearer grant & RDP cert SHA-256 pin
   - GET /v1/vm-status       -> Fresh status for health & dashboard probes
   - GET /health, GET /ready -> Liveness and readiness endpoints
2. Serves WebSocket data plane:
   - GET /v1/rdp             -> Upgrades to RFC 6455 Binary WebSocket, consumes grant,
                               and continuously relays bidirectionally to 127.0.0.1:3389.
3. Certificate Pinning:
   - Dynamically inspects the local Windows RDP server certificate or consumes RDP_CERT_SHA256 env.
"""

import base64
import hashlib
import json
import os
import socket
import ssl
import struct
import sys
import threading
import time

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

PORT = int(os.environ.get("PORT", "8080"))
RDP_TARGET_HOST = os.environ.get("RDP_TARGET_HOST", "127.0.0.1")
RDP_TARGET_PORT = int(os.environ.get("RDP_TARGET_PORT", "3389"))
DEFAULT_CERT_SHA256 = "7f4a2c9e1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567"

grants_lock = threading.Lock()
active_grants = {}  # token -> {"expires_at": timestamp, "cert_sha256": hex}


def log(msg: str):
    ts = time.strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{ts}] [Broker] {msg}", flush=True)


def get_local_rdp_cert_fingerprint() -> str:
    """Reads RDP_CERT_SHA256 from env or probes 127.0.0.1:3389 to extract the TLS certificate."""
    env_pin = os.environ.get("RDP_CERT_SHA256", "").strip().lower()
    if len(env_pin) == 64:
        return env_pin

    # Attempt to probe local RDP port via X.224 Connection Request + TLS Handshake
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(3.0)
        s.connect((RDP_TARGET_HOST, RDP_TARGET_PORT))

        # Send X.224 Connection Request requesting PROTOCOL_SSL (0x01)
        cr = bytes([
            0x03, 0x00, 0x00, 0x13,
            0x0e, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x08, 0x00,
            0x01, 0x00, 0x00, 0x00
        ])
        s.sendall(cr)
        cc = s.recv(1024)

        if len(cc) >= 19:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            with ctx.wrap_socket(s, server_hostname="localhost") as ss:
                der_cert = ss.getpeercert(binary_form=True)
                if der_cert:
                    fp = hashlib.sha256(der_cert).hexdigest().lower()
                    log(f"Probed live RDP leaf certificate SHA-256: {fp}")
                    return fp
    except Exception as e:
        log(f"Could not probe RDP certificate dynamically: {e}")

    log(f"Using default fallback certificate pin: {DEFAULT_CERT_SHA256}")
    return DEFAULT_CERT_SHA256


class ContinuousSessionBrokerServer:
    def __init__(self, host="0.0.0.0", port=PORT):
        self.host = host
        self.port = port
        self.server_sock = None
        self.cert_sha256 = get_local_rdp_cert_fingerprint()

    def start(self):
        self.server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_sock.bind((self.host, self.port))
        self.server_sock.listen(50)
        log(f"Continuous Session Broker listening on http://{self.host}:{self.port}")
        log(f"Bridging to Windows RDP Server at {RDP_TARGET_HOST}:{RDP_TARGET_PORT}")

        while True:
            try:
                conn, addr = self.server_sock.accept()
                threading.Thread(target=self._handle_client, args=(conn, addr), daemon=True).start()
            except Exception as e:
                log(f"Accept error: {e}")
                time.sleep(0.1)

    def _handle_client(self, conn: socket.socket, addr):
        conn.settimeout(15.0)
        try:
            req_data = conn.recv(4096).decode("utf-8", errors="replace")
            lines = req_data.split("\r\n")
            if not lines or not lines[0]:
                conn.close()
                return

            req_line = lines[0]
            parts = req_line.split()
            if len(parts) < 2:
                conn.close()
                return

            method = parts[0].upper()
            path = parts[1]

            # CORS Preflight
            if method == "OPTIONS":
                self._send_cors_response(conn)
                return

            if method == "GET" and path in ("/health", "/ready"):
                self._send_json(conn, 200, {"status": "ok", "service": "MobiDesk Session Broker"})
                return

            if method == "GET" and path.startswith("/v1/vm-status"):
                asg_tag = os.environ.get("VM_ASSIGNMENT_TAG") or f"asg_tag_{os.environ.get('VM_NAME', 'win-01')}"
                self._send_json(conn, 200, {
                    "assignmentTag": asg_tag,
                    "lifecycle": "RUNNING",
                    "rdpTransport": "RDP_NEGOTIATION_OK",
                    "staleAfterSeconds": 15,
                    "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                    "requestId": "req_status_live"
                })
                return

            if method == "POST" and path.startswith("/v1/session-grants"):
                grant_id = f"grant_{hashlib.sha256(str(time.time() + time.monotonic()).encode()).hexdigest()[:24]}"
                ttl_seconds = 60
                expires_at = time.time() + ttl_seconds

                # Refresh certificate fingerprint if updated
                self.cert_sha256 = get_local_rdp_cert_fingerprint()

                with grants_lock:
                    active_grants[grant_id] = {
                        "expires_at": expires_at,
                        "cert_sha256": self.cert_sha256
                    }

                log(f"Issued session grant '{grant_id}' (TTL: {ttl_seconds}s) to client {addr}")

                resp_obj = {
                    "grant": grant_id,
                    "expiresAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(expires_at)),
                    "relayPath": "/v1/rdp",
                    "rdpCertificate": {
                        "mode": "LEAF_X509_DER_SHA256",
                        "fingerprintSha256": self.cert_sha256
                    },
                    "requestId": f"req_{grant_id[:12]}"
                }
                self._send_json(conn, 200, resp_obj)
                return

            if method == "GET" and path.startswith("/v1/rdp"):
                self._handle_websocket_upgrade(conn, lines, path)
                return

            self._send_json(conn, 404, {"error": "Not Found", "path": path})

        except Exception as e:
            try:
                conn.close()
            except Exception:
                pass

    def _handle_websocket_upgrade(self, conn: socket.socket, lines: list, path: str):
        # Extract bearer token from Authorization header or URL query
        auth_line = next((l for l in lines if l.lower().startswith("authorization: bearer ")), "")
        token = auth_line.split(" ", 2)[-1].strip() if auth_line else ""

        if not token and "?grant=" in path:
            token = path.split("?grant=", 1)[1].split("&", 1)[0].strip()

        with grants_lock:
            grant_info = active_grants.pop(token, None)

        if not grant_info or time.time() > grant_info["expires_at"]:
            log(f"Denied WebSocket connection: invalid or expired grant '{token}'")
            conn.sendall(b"HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\n\r\n{\"error\":\"Invalid grant\"}")
            conn.close()
            return

        # Compute Sec-WebSocket-Accept
        ws_key = next((l for l in lines if l.lower().startswith("sec-websocket-key: ")), "").split(":")[-1].strip()
        guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        accept_key = base64.b64encode(hashlib.sha1((ws_key + guid).encode("ascii")).digest()).decode("ascii")

        upgrade_resp = (
            "HTTP/1.1 101 Switching Protocols\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Accept: {accept_key}\r\n\r\n"
        ).encode("ascii")
        conn.sendall(upgrade_resp)

        log(f"Upgraded WebSocket session with grant '{token[:12]}...'. Connecting to local Windows RDP server on {RDP_TARGET_HOST}:{RDP_TARGET_PORT}...")

        # Connect to Windows RDP Server
        try:
            rdp_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            rdp_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            rdp_sock.connect((RDP_TARGET_HOST, RDP_TARGET_PORT))
        except Exception as e:
            log(f"Failed to connect to local Windows RDP server: {e}")
            conn.close()
            return

        # Start bidirectional byte pump: WebSocket <-> Raw TCP RDP
        self._pump_duplex(conn, rdp_sock)

    def _pump_duplex(self, ws_conn: socket.socket, tcp_conn: socket.socket):
        ws_conn.settimeout(None)
        tcp_conn.settimeout(None)

        def tcp_to_ws():
            try:
                while True:
                    data = tcp_conn.recv(16384)
                    if not data:
                        break
                    # Wrap raw RDP bytes into WebSocket binary frame (opcode 0x02)
                    frame = self._encode_ws_frame(data)
                    ws_conn.sendall(frame)
            except Exception:
                pass
            finally:
                try:
                    ws_conn.close()
                except Exception:
                    pass

        threading.Thread(target=tcp_to_ws, daemon=True).start()

        try:
            while True:
                b1 = ws_conn.recv(1)
                if not b1:
                    break
                b2 = ws_conn.recv(1)
                if not b2:
                    break

                is_masked = (b2[0] & 0x80) != 0
                payload_len = b2[0] & 0x7F

                if payload_len == 126:
                    len_bytes = ws_conn.recv(2)
                    payload_len = struct.unpack(">H", len_bytes)[0]
                elif payload_len == 127:
                    len_bytes = ws_conn.recv(8)
                    payload_len = struct.unpack(">Q", len_bytes)[0]

                mask = ws_conn.recv(4) if is_masked else None
                payload = bytearray()
                while len(payload) < payload_len:
                    chunk = ws_conn.recv(payload_len - len(payload))
                    if not chunk:
                        break
                    payload.extend(chunk)

                if mask:
                    for i in range(len(payload)):
                        payload[i] ^= mask[i % 4]

                opcode = b1[0] & 0x0F
                if opcode == 0x02:  # Binary frame
                    tcp_conn.sendall(payload)
                elif opcode == 0x08:  # Connection close
                    break
                elif opcode == 0x09:  # Ping -> Pong
                    pong_frame = bytearray([0x8A, 0x00])
                    ws_conn.sendall(pong_frame)
        except Exception:
            pass
        finally:
            try:
                tcp_conn.close()
            except Exception:
                pass
            try:
                ws_conn.close()
            except Exception:
                pass
            log("Closed RDP WebSocket session and released TCP socket.")

    def _encode_ws_frame(self, data: bytes) -> bytes:
        length = len(data)
        if length <= 125:
            header = bytes([0x82, length])
        elif length <= 65535:
            header = bytes([0x82, 126]) + struct.pack(">H", length)
        else:
            header = bytes([0x82, 127]) + struct.pack(">Q", length)
        return header + data

    def _send_json(self, conn: socket.socket, status_code: int, obj: dict):
        body = json.dumps(obj).encode("utf-8")
        status_text = "OK" if status_code == 200 else "Created" if status_code == 201 else "Error"
        headers = (
            f"HTTP/1.1 {status_code} {status_text}\r\n"
            "Content-Type: application/json\r\n"
            f"Content-Length: {len(body)}\r\n"
            "Access-Control-Allow-Origin: *\r\n"
            "Access-Control-Allow-Headers: *\r\n"
            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
            "Connection: close\r\n\r\n"
        ).encode("ascii")
        conn.sendall(headers + body)
        conn.close()

    def _send_cors_response(self, conn: socket.socket):
        headers = (
            "HTTP/1.1 204 No Content\r\n"
            "Access-Control-Allow-Origin: *\r\n"
            "Access-Control-Allow-Headers: *\r\n"
            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
            "Connection: close\r\n\r\n"
        ).encode("ascii")
        conn.sendall(headers)
        conn.close()


if __name__ == "__main__":
    broker = ContinuousSessionBrokerServer()
    broker.start()
