/**
 * Writes a minimal 16×16 32bpp favicon.ico (navy #0B1F3B + gold accent) — no dependencies.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const out = path.join(__dirname, '..', 'public', 'favicon.ico');

const W = 16;
const H = 16;
// BGRA, bottom-up row order for DIB
const bgra = (b, g, r, a = 255) => Buffer.from([b, g, r, a]);
const navy = bgra(0x3b, 0x1f, 0x0b);
const gold = bgra(0x74, 0xa5, 0xc4);
const white = bgra(0xff, 0xff, 0xff, 0xff);

/** Simple bar heights (0–15), left to right */
const bars = [
  { x: 1, w: 3, h: 6, c: navy },
  { x: 5, w: 3, h: 9, c: gold },
  { x: 9, w: 4, h: 14, c: navy },
  { x: 14, w: 3, h: 10, c: navy },
];

const row = Buffer.alloc(W * 4, 0xff);
const xor = [];
for (let y = H - 1; y >= 0; y--) {
  row.fill(0xff);
  for (const b of bars) {
    const top = H - 1 - b.h;
    if (y >= top && y <= H - 1) {
      for (let x = b.x; x < b.x + b.w && x < W; x++) {
        b.c.copy(row, x * 4);
      }
    }
  }
  xor.push(Buffer.from(row));
}
const xorBuf = Buffer.concat(xor);

// AND mask: 1 bit per pixel, 0 = opaque for 32bpp; rows padded to 32 bits
const andStride = Math.ceil(W / 32) * 4;
const andMask = Buffer.alloc(andStride * H, 0);

const biSize = 40;
const biWidth = W;
const biHeight = H * 2; // XOR + AND
const header = Buffer.alloc(biSize);
header.writeUInt32LE(40, 0);
header.writeInt32LE(biWidth, 4);
header.writeInt32LE(biHeight, 8);
header.writeUInt16LE(1, 12);
header.writeUInt16LE(32, 14);
header.writeUInt32LE(0, 16);
header.writeUInt32LE(xorBuf.length + andMask.length, 20);

const image = Buffer.concat([header, xorBuf, andMask]);
const offset = 6 + 16;
const idCount = 1;

const dir = Buffer.alloc(6 + 16);
dir.writeUInt16LE(0, 0);
dir.writeUInt16LE(1, 2);
dir.writeUInt16LE(idCount, 4);
// ICONDIRENTRY
dir.writeUInt8(W, 6);
dir.writeUInt8(H, 7);
dir.writeUInt8(0, 8);
dir.writeUInt8(0, 9);
dir.writeUInt16LE(1, 10);
dir.writeUInt16LE(32, 12);
dir.writeUInt32LE(image.length, 14);
dir.writeUInt32LE(offset, 18);

const ico = Buffer.concat([dir, image]);
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, ico);
console.log('Wrote', out, ico.length, 'bytes');
