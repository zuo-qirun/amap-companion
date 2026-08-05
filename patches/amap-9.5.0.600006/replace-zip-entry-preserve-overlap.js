"use strict";

const fs = require("fs");
const path = require("path");

const EOCD_SIGNATURE = 0x06054b50;
const CENTRAL_SIGNATURE = 0x02014b50;
const LOCAL_SIGNATURE = 0x04034b50;
const MAX_EOCD_SEARCH = 22 + 0xffff;

function readAt(fd, length, position) {
  const buffer = Buffer.alloc(length);
  let offset = 0;
  while (offset < length) {
    const count = fs.readSync(fd, buffer, offset, length - offset, position + offset);
    if (count === 0) {
      throw new Error(`Unexpected EOF at ${position + offset}`);
    }
    offset += count;
  }
  return buffer;
}

function readEocd(fd, fileSize) {
  const tailLength = Math.min(fileSize, MAX_EOCD_SEARCH);
  const tailOffset = fileSize - tailLength;
  const tail = readAt(fd, tailLength, tailOffset);
  for (let offset = tail.length - 22; offset >= 0; offset -= 1) {
    if (tail.readUInt32LE(offset) !== EOCD_SIGNATURE) {
      continue;
    }
    const commentLength = tail.readUInt16LE(offset + 20);
    if (offset + 22 + commentLength !== tail.length) {
      continue;
    }
    const diskNumber = tail.readUInt16LE(offset + 4);
    const centralDisk = tail.readUInt16LE(offset + 6);
    const diskEntries = tail.readUInt16LE(offset + 8);
    const totalEntries = tail.readUInt16LE(offset + 10);
    if (diskNumber !== 0 || centralDisk !== 0 || diskEntries !== totalEntries
        || totalEntries === 0xffff) {
      throw new Error("Multi-disk and ZIP64 archives are not supported");
    }
    return {
      entryCount: totalEntries,
      centralSize: tail.readUInt32LE(offset + 12),
      centralOffset: tail.readUInt32LE(offset + 16),
      comment: Buffer.from(tail.subarray(offset + 22, offset + 22 + commentLength)),
    };
  }
  throw new Error("ZIP end-of-central-directory record not found");
}

function readCentralEntries(fd, eocd) {
  const central = readAt(fd, eocd.centralSize, eocd.centralOffset);
  const entries = [];
  let offset = 0;
  while (offset < central.length) {
    if (offset + 46 > central.length || central.readUInt32LE(offset) !== CENTRAL_SIGNATURE) {
      throw new Error(`Invalid central-directory entry at relative offset ${offset}`);
    }
    const nameLength = central.readUInt16LE(offset + 28);
    const extraLength = central.readUInt16LE(offset + 30);
    const commentLength = central.readUInt16LE(offset + 32);
    const recordLength = 46 + nameLength + extraLength + commentLength;
    const record = Buffer.from(central.subarray(offset, offset + recordLength));
    entries.push({
      name: record.subarray(46, 46 + nameLength).toString("utf8"),
      flags: record.readUInt16LE(8),
      method: record.readUInt16LE(10),
      crc32: record.readUInt32LE(16),
      compressedSize: record.readUInt32LE(20),
      uncompressedSize: record.readUInt32LE(24),
      localOffset: record.readUInt32LE(42),
      record,
    });
    offset += recordLength;
  }
  if (offset !== central.length || entries.length !== eocd.entryCount) {
    throw new Error(`Central-directory count mismatch: ${entries.length}/${eocd.entryCount}`);
  }
  return entries;
}

function openZip(apkPath) {
  const fd = fs.openSync(apkPath, "r");
  const fileSize = fs.fstatSync(fd).size;
  const eocd = readEocd(fd, fileSize);
  const entries = readCentralEntries(fd, eocd);
  return { fd, fileSize, eocd, entries };
}

function closeZip(zip) {
  fs.closeSync(zip.fd);
}

function buildReplacementLocalEntry(donor, entry, targetOffset) {
  const fixed = readAt(donor.fd, 30, entry.localOffset);
  if (fixed.readUInt32LE(0) !== LOCAL_SIGNATURE) {
    throw new Error(`Invalid donor local header for ${entry.name}`);
  }
  const localNameLength = fixed.readUInt16LE(26);
  const localExtraLength = fixed.readUInt16LE(28);
  const variable = readAt(
    donor.fd,
    localNameLength + localExtraLength,
    entry.localOffset + 30,
  );
  const localName = variable.subarray(0, localNameLength);
  if (localName.toString("utf8") !== entry.name) {
    throw new Error(`Donor local/central name mismatch for ${entry.name}`);
  }
  let localExtra = Buffer.from(variable.subarray(localNameLength));
  if (entry.method === 0 && Number.isInteger(targetOffset)) {
    const dataOffset = targetOffset + 30 + localNameLength + localExtra.length;
    const alignment = (4 - (dataOffset % 4)) % 4;
    if (alignment !== 0) {
      const alignmentExtra = Buffer.alloc(4 + alignment);
      alignmentExtra.writeUInt16LE(0xd935, 0);
      alignmentExtra.writeUInt16LE(alignment, 2);
      localExtra = Buffer.concat([localExtra, alignmentExtra]);
    }
  }
  const compressed = readAt(
    donor.fd,
    entry.compressedSize,
    entry.localOffset + 30 + localNameLength + localExtraLength,
  );

  const header = Buffer.from(fixed);
  const flagsWithoutDescriptor = entry.flags & ~0x0008;
  header.writeUInt16LE(flagsWithoutDescriptor, 6);
  header.writeUInt32LE(entry.crc32, 14);
  header.writeUInt32LE(entry.compressedSize, 18);
  header.writeUInt32LE(entry.uncompressedSize, 22);
  header.writeUInt16LE(localExtra.length, 28);

  return {
    flags: flagsWithoutDescriptor,
    bytes: Buffer.concat([header, localName, localExtra, compressed]),
  };
}

function buildEocd(entryCount, centralSize, centralOffset, comment) {
  const eocd = Buffer.alloc(22 + comment.length);
  eocd.writeUInt32LE(EOCD_SIGNATURE, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(entryCount, 8);
  eocd.writeUInt16LE(entryCount, 10);
  eocd.writeUInt32LE(centralSize, 12);
  eocd.writeUInt32LE(centralOffset, 16);
  eocd.writeUInt16LE(comment.length, 20);
  comment.copy(eocd, 22);
  return eocd;
}

function contentKey(entry) {
  return [
    entry.name,
    entry.method,
    entry.crc32,
    entry.compressedSize,
    entry.uncompressedSize,
  ].join("\u0000");
}

function isV1SignatureEntry(name) {
  const upper = name.toUpperCase();
  return upper === "META-INF/MANIFEST.MF"
      || /^META-INF\/[^/]+\.(SF|RSA|DSA|EC)$/.test(upper);
}

function main() {
  if (process.argv.length !== 6) {
    throw new Error(
      "Usage: node replace-zip-entry-preserve-overlap.js "
      + "<base-overlap.apk> <rebuilt-unsigned.apk> <rebuilt-signed.apk> <output.apk>",
    );
  }
  const inputPath = path.resolve(process.argv[2]);
  const donorPath = path.resolve(process.argv[3]);
  const signedDonorPath = path.resolve(process.argv[4]);
  const outputPath = path.resolve(process.argv[5]);
  if (fs.existsSync(outputPath)) {
    throw new Error(`Refusing to overwrite existing output: ${outputPath}`);
  }

  const input = openZip(inputPath);
  const donor = openZip(donorPath);
  const signedDonor = openZip(signedDonorPath);
  try {
    const inputByContent = new Map();
    for (const entry of input.entries) {
      const key = contentKey(entry);
      if (!inputByContent.has(key)) {
        inputByContent.set(key, entry);
      }
    }

    const signatureEntries = signedDonor.entries.filter(
      (entry) => isV1SignatureEntry(entry.name),
    );
    if (!signatureEntries.some((entry) => entry.name.toUpperCase() === "META-INF/MANIFEST.MF")
        || !signatureEntries.some((entry) => entry.name.toUpperCase().endsWith(".SF"))
        || !signatureEntries.some((entry) => /\.(RSA|DSA|EC)$/i.test(entry.name))) {
      throw new Error("Signed donor does not contain a complete v1 signature");
    }

    fs.copyFileSync(inputPath, outputPath);
    const outputFd = fs.openSync(outputPath, "a");
    try {
      let nextOffset = input.fileSize;
      let reused = 0;
      let appended = 0;
      const centralRecords = [];

      for (const entry of donor.entries) {
        if (isV1SignatureEntry(entry.name)) {
          continue;
        }
        const existing = inputByContent.get(contentKey(entry));
        if (existing) {
          centralRecords.push(existing.record);
          reused += 1;
          continue;
        }

        const replacement = buildReplacementLocalEntry(donor, entry, nextOffset);
        fs.writeSync(outputFd, replacement.bytes);
        const record = Buffer.from(entry.record);
        record.writeUInt16LE(replacement.flags, 8);
        record.writeUInt32LE(nextOffset, 42);
        centralRecords.push(record);
        nextOffset += replacement.bytes.length;
        appended += 1;
      }

      for (const entry of signatureEntries) {
        const replacement = buildReplacementLocalEntry(signedDonor, entry, nextOffset);
        fs.writeSync(outputFd, replacement.bytes);
        const record = Buffer.from(entry.record);
        record.writeUInt16LE(replacement.flags, 8);
        record.writeUInt32LE(nextOffset, 42);
        centralRecords.push(record);
        nextOffset += replacement.bytes.length;
      }

      const central = Buffer.concat(centralRecords);
      const centralOffset = nextOffset;
      fs.writeSync(outputFd, central);
      fs.writeSync(
        outputFd,
        buildEocd(centralRecords.length, central.length, centralOffset, donor.eocd.comment),
      );
      console.log(`Reused base entries: ${reused}`);
      console.log(`Appended rebuilt entries: ${appended}`);
      console.log(`Appended v1 signature entries: ${signatureEntries.length}`);
    } finally {
      fs.closeSync(outputFd);
    }

    console.log(`Input bytes:  ${input.fileSize}`);
    console.log(`Output bytes: ${fs.statSync(outputPath).size}`);
  } finally {
    closeZip(input);
    closeZip(donor);
    closeZip(signedDonor);
  }
}

try {
  main();
} catch (error) {
  console.error(error && error.stack ? error.stack : String(error));
  process.exitCode = 1;
}
