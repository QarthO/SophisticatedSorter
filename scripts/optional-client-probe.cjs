// Protocol smoke test; see docs/optional-clients-fabric-1.21.1.md.
const mc = require("minecraft-protocol");
const assert = require("node:assert/strict");
function vi(n) {
  const b = [];
  do {
    let v = n & 127;
    n >>>= 7;
    if (n) v |= 128;
    b.push(v);
  } while (n);
  return Buffer.from(b);
}
function str(s) {
  const b = Buffer.from(s);
  return Buffer.concat([vi(b.length), b]);
}
function readMap(buf) {
  let p = 0;
  const v = () => {
    let n = 0,
      s = 0,
      b;
    do {
      b = buf[p++];
      n |= (b & 127) << s;
      s += 7;
    } while (b & 128);
    return n;
  };
  const s = () => {
    const n = v(),
      r = buf.toString("utf8", p, p + n);
    p += n;
    return r;
  };
  const result = {};
  for (let a = v(); a > 0; a--) {
    const ns = s() || "minecraft";
    for (let b = v(); b > 0; b--) {
      const key = ns + ":" + s(),
        map = {};
      let last = 0;
      for (let c = v(); c > 0; c--) {
        const ins = s() || "minecraft";
        for (let d = v(); d > 0; d--) {
          let raw = last + v() - 1;
          for (let e = v(); e > 0; e--) {
            map[ins + ":" + s()] = ++raw;
          }
          last = raw;
        }
      }
      result[key] = map;
    }
  }
  return result;
}
async function probe(stock) {
  return new Promise((resolve, reject) => {
    const client = mc.createClient({
      host: "127.0.0.1",
      port: 25579,
      username: stock ? "StockProbe" : "NoSorterProbe",
      version: "1.21.1",
      auth: "offline",
    });
    const channels = stock
      ? [
          "sophisticatedsorter:tracked_container_key",
          "sophisticatedsorter:container_settings_contents",
        ]
      : [];
    const chunks = [];
    let checked = false;
    let done = false;
    const timeout = setTimeout(() => finish(new Error("Timed out")), 15000);
    const finish = (e) => {
      if (done) return;
      done = true;
      clearTimeout(timeout);
      client.end();
      e ? reject(e) : resolve();
    };
    client.on("error", (e) => finish(e));
    client.on("disconnect", (p) => finish(new Error(JSON.stringify(p))));
    client.on("ping", (p) => client.write("pong", { id: p.id }));
    client.on("custom_payload", (p) => {
      try {
        if (p.channel === "minecraft:register") {
          const receive =
            client.state === "configuration"
              ? ["c:version", "c:register", "fabric:registry/sync/direct"]
              : channels;
          client.write("custom_payload", {
            channel: "minecraft:register",
            data: Buffer.from(receive.join("\0")),
          });
        } else if (p.channel === "c:version")
          client.write("custom_payload", {
            channel: p.channel,
            data: Buffer.from([1, 1]),
          });
        else if (p.channel === "c:register")
          client.write("custom_payload", {
            channel: p.channel,
            data: Buffer.concat([
              vi(1),
              str("play"),
              vi(channels.length),
              ...channels.map(str),
            ]),
          });
        else if (p.channel === "fabric:registry/sync/direct") {
          if (p.data.length) chunks.push(p.data);
          else {
            const map = readMap(Buffer.concat(chunks));
            const menus = map["minecraft:menu"];
            assert.equal(
              Object.hasOwn(menus, "sophisticatedsorter:container_settings"),
              stock,
            );
            assert.deepEqual(
              Object.values(menus).sort((a, b) => a - b),
              Array.from({ length: Object.keys(menus).length }, (_, i) => i),
            );
            assert(Object.hasOwn(menus, "sophisticatedbackpacks:backpack"));
            checked = true;
            client.write("custom_payload", {
              channel: "fabric:registry/sync/complete",
              data: Buffer.alloc(0),
            });
          }
        } else if (p.channel.startsWith("sophisticatedsorter:") && !stock)
          throw new Error("Sorter packet sent to absent client");
      } catch (e) {
        finish(e);
      }
    });
    client.once("playerJoin", () => {
      try {
        assert(checked, "Registry sync was not exercised");
        console.log(
          `${stock ? "Stock-channel" : "No-sorter"} probe: registry sync and play-state join PASS`,
        );
        finish();
      } catch (e) {
        finish(e);
      }
    });
  });
}
(async () => {
  await probe(false);
  await probe(true);
  await probe(false);
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
