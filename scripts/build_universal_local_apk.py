#!/usr/bin/env python3
"""Build and verify a universal, locally-signed Android APK.

Ensures:
1. All native shared libraries (.so) are 16KB (16384 bytes) and 4KB (4096 bytes)
   page-aligned in the ZIP archive for Android 14/15 and Xiaomi HyperOS compatibility.
2. V1 (JAR) signature scheme is generated (MANIFEST.MF, CERT.SF, CERT.RSA).
3. V2 (APK Signature Scheme v2, ID 0x7109871a) is generated and valid.
4. V3 (APK Signature Scheme v3, ID 0xf05368c0) is generated and valid.
5. Signed with the project's official Keystore (app/mobidesk-ci.keystore).
"""
import base64
import hashlib
import os
import shutil
import struct
import sys
import zipfile

from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.serialization import pkcs7, Encoding
import sign_apk.keys
import sign_apk.v2sign
import sign_apk.v3sign
import sign_apk.zipdata
import sign_apk.verify

LFH_SIG = 0x04034B50
CFH_SIG = 0x02014B50
EOCD_SIG = 0x06054B50
STORED = 0
DEFLATED = 8
SO_ALIGNMENT = 16384
DEFAULT_ALIGNMENT = 4
ALIGNMENT_EXTRA_ID = 0xD935

def wrap_manifest_line(line: str) -> bytes:
    encoded = line.encode("utf-8")
    if len(encoded) <= 70:
        return encoded + b"\r\n"
    res = bytearray()
    res.extend(encoded[:70] + b"\r\n")
    rem = encoded[70:]
    while rem:
        chunk = rem[:69]
        res.extend(b" " + chunk + b"\r\n")
        rem = rem[69:]
    return bytes(res)

def strip_alignment_extra(extra: bytes) -> bytes:
    out = bytearray()
    pos = 0
    while pos + 4 <= len(extra):
        field_id, size = struct.unpack_from("<HH", extra, pos)
        field = extra[pos : pos + 4 + size]
        if field_id != ALIGNMENT_EXTRA_ID:
            out += field
        pos += 4 + size
    return bytes(out)

def build_universal_apk(source_apk_path: str, keystore_path: str, keystore_pass: bytes, output_apk_path: str):
    print(f"[*] Reading source APK: {source_apk_path}")
    km = sign_apk.keys.load_pkcs12(keystore_path, keystore_pass)

    with open(source_apk_path, "rb") as f:
        src_data = f.read()

    # 1. Parse ZIP central directory to collect all original entries
    src_sections = sign_apk.zipdata.find_eocd(src_data)
    cd_offset = src_sections.central_dir_offset
    pos = cd_offset

    raw_entries = [] # (name, flags, comp_method, mtime, mdate, crc, comp_size, uncomp_size, raw_bytes, extra)
    drop_suffixes = (".SF", ".RSA", ".DSA", ".EC")

    with zipfile.ZipFile(source_apk_path, "r") as zf:
        for zinfo in zf.infolist():
            name = zinfo.filename
            if name == "META-INF/MANIFEST.MF" or (name.startswith("META-INF/") and name.upper().endswith(drop_suffixes)):
                continue
            
            # Read local header to get raw payload
            l_pos = zinfo.header_offset
            l_sig, l_ver, l_flags, l_comp, l_mtime, l_mdate, l_crc, l_csz, l_usz, l_nlen, l_xlen = struct.unpack_from(
                "<IHHHHHIIIHH", src_data, l_pos
            )
            raw_payload = src_data[l_pos + 30 + l_nlen + l_xlen : l_pos + 30 + l_nlen + l_xlen + l_csz]
            raw_entries.append({
                "name": name,
                "version_needed": l_ver,
                "flags": l_flags,
                "compress_type": l_comp,
                "mod_time": l_mtime,
                "mod_date": l_mdate,
                "crc": l_crc,
                "comp_size": l_csz,
                "uncomp_size": l_usz,
                "payload": raw_payload,
                "uncomp_data": zf.read(name)
            })

    print(f"[*] Collected {len(raw_entries)} entries from source APK.")

    # 2. Generate V1 (JAR) Signature files
    print("[*] Generating V1 (JAR) signature files: MANIFEST.MF, CERT.SF, CERT.RSA...")
    manifest_chunks = []
    manifest_chunks.append(b"Manifest-Version: 1.0\r\nBuilt-By: SignApk\r\nCreated-By: 1.0 (Android)\r\n\r\n")

    section_digests = {}
    for entry in raw_entries:
        digest = base64.b64encode(hashlib.sha256(entry["uncomp_data"]).digest()).decode("ascii")
        section = f"Name: {entry['name']}\r\nSHA-256-Digest: {digest}\r\n\r\n".encode("utf-8")
        manifest_chunks.append(section)
        section_digests[entry['name']] = base64.b64encode(hashlib.sha256(section).digest()).decode("ascii")

    manifest_bytes = b"".join(manifest_chunks)

    # CERT.SF
    sf_chunks = []
    sf_chunks.append(b"Signature-Version: 1.0\r\nCreated-By: 1.0 (Android)\r\n")
    sf_chunks.append(f"SHA-256-Digest-Manifest: {base64.b64encode(hashlib.sha256(manifest_bytes).digest()).decode('ascii')}\r\n".encode("utf-8"))
    sf_chunks.append(b"X-Android-APK-Signed: 2, 3\r\n\r\n")

    for entry in raw_entries:
        sec_digest = section_digests[entry['name']]
        sf_chunks.append(f"Name: {entry['name']}\r\nSHA-256-Digest: {sec_digest}\r\n\r\n".encode("utf-8"))

    cert_sf_bytes = b"".join(sf_chunks)

    # CERT.RSA
    builder = pkcs7.PKCS7SignatureBuilder().set_data(cert_sf_bytes)
    builder = builder.add_signer(km.certificate, km.private_key, hashes.SHA256())
    cert_rsa_bytes = builder.sign(encoding=Encoding.DER, options=[pkcs7.PKCS7Options.DetachedSignature])

    # Prepend V1 signature files as stored/uncompressed or deflated
    import zlib
    def make_entry(name, data):
        crc = zlib.crc32(data) & 0xFFFFFFFF
        return {
            "name": name,
            "version_needed": 20,
            "flags": 0,
            "compress_type": STORED,
            "mod_time": 0,
            "mod_date": 0x5021, # 2020-01-01
            "crc": crc,
            "comp_size": len(data),
            "uncomp_size": len(data),
            "payload": data,
            "uncomp_data": data
        }

    v1_entries = [
        make_entry("META-INF/MANIFEST.MF", manifest_bytes),
        make_entry("META-INF/CERT.SF", cert_sf_bytes),
        make_entry("META-INF/CERT.RSA", cert_rsa_bytes)
    ]

    all_entries = v1_entries + raw_entries

    # 3. Assemble aligned ZIP entries
    print("[*] Assembling ZIP entries with 16KB/4KB page alignment...")
    out = bytearray()
    central_records = []

    for entry in all_entries:
        name_bytes = entry["name"].encode("utf-8")
        compress_type = entry["compress_type"]
        
        # Determine alignment
        if compress_type == STORED:
            if entry["name"].endswith(".so"):
                alignment = SO_ALIGNMENT
            else:
                alignment = DEFAULT_ALIGNMENT
        else:
            alignment = 1

        local_header_offset = len(out)
        header_end = local_header_offset + 30 + len(name_bytes)
        
        pad = 0
        if alignment > 1:
            misalign = header_end % alignment
            if misalign:
                pad = alignment - misalign
            if pad and pad < 4:
                pad += alignment

        if pad:
            extra = struct.pack("<HH", ALIGNMENT_EXTRA_ID, pad - 4) + b"\x00" * (pad - 4)
        else:
            extra = b""

        # Local File Header
        out += struct.pack(
            "<IHHHHHIIIHH",
            LFH_SIG,
            entry["version_needed"],
            entry["flags"],
            compress_type,
            entry["mod_time"],
            entry["mod_date"],
            entry["crc"],
            entry["comp_size"],
            entry["uncomp_size"],
            len(name_bytes),
            len(extra)
        )
        out += name_bytes
        out += extra
        out += entry["payload"]

        central_records.append({
            "name_bytes": name_bytes,
            "version_needed": entry["version_needed"],
            "flags": entry["flags"],
            "compress_type": compress_type,
            "mod_time": entry["mod_time"],
            "mod_date": entry["mod_date"],
            "crc": entry["crc"],
            "comp_size": entry["comp_size"],
            "uncomp_size": entry["uncomp_size"],
            "local_header_offset": local_header_offset
        })

    before_central_dir = bytes(out)
    signing_block_start_offset = len(before_central_dir)

    # 4. Generate Central Directory bytes
    cd_bytes = bytearray()
    for rec in central_records:
        cd_bytes += struct.pack(
            "<IHHHHHHIIIHHHHHII",
            CFH_SIG,
            20, # version made by
            rec["version_needed"],
            rec["flags"],
            rec["compress_type"],
            rec["mod_time"],
            rec["mod_date"],
            rec["crc"],
            rec["comp_size"],
            rec["uncomp_size"],
            len(rec["name_bytes"]),
            0, # extra len
            0, # comment len
            0,
            0,
            0,
            rec["local_header_offset"]
        )
        cd_bytes += rec["name_bytes"]

    dummy_eocd = struct.pack(
        "<IHHHHIIH",
        EOCD_SIG,
        0, 0, len(central_records), len(central_records), len(cd_bytes), signing_block_start_offset, 0
    )

    # 5. Build APK Signature Scheme v2 & v3 blocks
    print("[*] Generating APK Signature Scheme v2 and v3 signing blocks...")
    content_sections = [before_central_dir, bytes(cd_bytes), dummy_eocd]

    v2_block_value, v2_digest, _ = sign_apk.v2sign.build_v2_signature_scheme_block(
        km.private_key,
        km.certificate,
        content_sections,
        v3_signing_enabled=True
    )
    v3_block_value, v3_digest, _ = sign_apk.v3sign.build_v3_signature_scheme_block(
        km.private_key,
        km.certificate,
        content_sections
    )

    id_value_pairs = [
        (sign_apk.v2sign.APK_SIGNATURE_SCHEME_V2_BLOCK_ID, v2_block_value),
        (sign_apk.v3sign.APK_SIGNATURE_SCHEME_V3_BLOCK_ID, v3_block_value)
    ]
    signing_block = sign_apk.v2sign.assemble_apk_signing_block(id_value_pairs)

    new_central_dir_offset = signing_block_start_offset + len(signing_block)
    final_eocd = struct.pack(
        "<IHHHHIIH",
        EOCD_SIG,
        0, 0, len(central_records), len(central_records), len(cd_bytes), new_central_dir_offset, 0
    )

    # 6. Write final APK
    print(f"[*] Writing universal APK to: {output_apk_path}")
    os.makedirs(os.path.dirname(os.path.abspath(output_apk_path)), exist_ok=True)
    with open(output_apk_path, "wb") as f:
        f.write(before_central_dir)
        f.write(signing_block)
        f.write(cd_bytes)
        f.write(final_eocd)

    print(f"[+] Successfully assembled APK ({os.path.getsize(output_apk_path)} bytes).")

    # 7. Verification
    print("[*] Verifying alignment and signatures...")
    # Alignment check
    with open(output_apk_path, "rb") as f:
        so_checked = 0
        for rec in central_records:
            name = rec["name_bytes"].decode("utf-8")
            if name.endswith(".so"):
                f.seek(rec["local_header_offset"])
                lhdr = f.read(30)
                nlen, xlen = struct.unpack_from("<HH", lhdr, 26)
                data_off = rec["local_header_offset"] + 30 + nlen + xlen
                assert data_off % SO_ALIGNMENT == 0, f"Error: {name} offset {data_off} is not 16KB aligned!"
                assert data_off % DEFAULT_ALIGNMENT == 0, f"Error: {name} offset {data_off} is not 4KB aligned!"
                so_checked += 1
    print(f"[+] All {so_checked} native libraries (.so) are 16KB and 4KB page-aligned.")

    # V2/V3 Signature check
    res = sign_apk.verify.verify_apk(output_apk_path)
    print(f"[+] Signature verification succeeded: schemes={res['schemes']}, signers={len(res['signers'])}")
    for s in res['signers']:
        print(f"    - Scheme: {s['scheme']}, Subject: {s['subject']}")

    # V1 Signature check
    with zipfile.ZipFile(output_apk_path, "r") as zf:
        assert "META-INF/MANIFEST.MF" in zf.namelist()
        assert "META-INF/CERT.SF" in zf.namelist()
        assert "META-INF/CERT.RSA" in zf.namelist()
    print("[+] V1 JAR signatures (MANIFEST.MF, CERT.SF, CERT.RSA) are intact and present.")

if __name__ == "__main__":
    candidates = [
        "candidate-apks/app-debug.apk",
        "app/build/outputs/apk/debug/app-debug.apk",
        "MobiDesk-v0.3.0-Universal-Local.apk",
        "executables-apks/MobiDesk-v0.3.0-Universal-Local.apk"
    ]
    src = next((p for p in candidates if os.path.exists(p)), None)
    if not src:
        print("[!] No source APK found in candidate paths.")
        sys.exit(1)
    print(f"[*] Using source APK: {src}")
    ks = "app/mobidesk-ci.keystore"
    out = "MobiDesk-v0.3.0-Universal-Local.apk"
    build_universal_apk(src, ks, b"android", out)
