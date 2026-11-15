import zipfile
import struct
import sys

def parse_manifest(apk_path):
    with zipfile.ZipFile(apk_path, 'r') as z:
        b = z.read('AndroidManifest.xml')

    sp_chunk_size = struct.unpack('<I', b[12:16])[0]
    string_count = struct.unpack('<I', b[16:20])[0]
    flags = struct.unpack('<I', b[24:28])[0]
    strings_start = struct.unpack('<I', b[28:32])[0]
    is_utf8 = bool(flags & (1 << 8))
    offsets = [struct.unpack('<I', b[36 + i*4:40 + i*4])[0] for i in range(string_count)]
    strings = []
    pool_data = b[8 + strings_start:]
    for o in offsets:
        if is_utf8:
            idx = o
            l1 = pool_data[idx]; idx += 1
            if l1 & 0x80: idx += 1
            l2 = pool_data[idx]; idx += 1
            if l2 & 0x80: idx += 1
            strings.append(pool_data[idx:idx+l2].decode('utf-8', errors='replace'))
        else:
            l = struct.unpack('<H', pool_data[o:o+2])[0]
            strings.append(pool_data[o+2:o+2+l*2].decode('utf-16le', errors='replace'))

    idx = 8 + sp_chunk_size
    if struct.unpack('<H', b[idx:idx+2])[0] == 0x0180:
        res_chunk_size = struct.unpack('<I', b[idx+4:idx+8])[0]
        idx += res_chunk_size

    indent = 0
    while idx < len(b):
        chunk_type = struct.unpack('<H', b[idx:idx+2])[0]
        chunk_size = struct.unpack('<I', b[idx+4:idx+8])[0]
        if chunk_type == 0x0102: # START_TAG
            tag_ns_idx, tag_name_idx, attr_start, attr_size, attr_count = struct.unpack('<IIHHH', b[idx+16:idx+30])
            tag = strings[tag_name_idx]
            cur = idx + 16 + attr_start
            attr_strs = []
            for i in range(attr_count):
                a_ns, a_name, a_raw, a_sz, a_res0, a_type, a_data = struct.unpack('<IIIHBBI', b[cur:cur+20])
                name = strings[a_name]
                raw = strings[a_raw] if a_raw != 0xffffffff else None
                if a_type == 0x10: val = str(a_data)
                elif a_type == 0x12: val = 'true' if a_data != 0 else 'false'
                elif a_type == 0x03: val = strings[a_data] if a_data < len(strings) else str(a_data)
                elif raw: val = raw
                else: val = hex(a_data)
                attr_strs.append(f'{name}="{val}"')
                cur += attr_size
            print('  '*indent + f'<{tag} ' + ' '.join(attr_strs) + '>')
            indent += 1
        elif chunk_type == 0x0103: # END_TAG
            indent -= 1
            tag_name_idx = struct.unpack('<I', b[idx+20:idx+24])[0]
            print('  '*indent + f'</{strings[tag_name_idx]}>')
        idx += chunk_size

if __name__ == '__main__':
    parse_manifest(sys.argv[1] if len(sys.argv) > 1 else 'executables-apks/fp-05-to-11-freerdp-pipeline/app-debug.apk')
