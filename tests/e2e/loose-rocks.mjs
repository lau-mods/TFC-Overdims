const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode spectator');
  command(client, 'gamerule doMobSpawning false');
  command(client, 'gamerule sendCommandFeedback true');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  command(client, 'scoreboard objectives add m02probe dummy');
  command(client, 'gamerule commandModificationBlockLimit 1048576');

  async function capture(position, acId, label) {
    mct(client, 'input', 'key', 'press', 'f1');
    try {
      await camera(client, position);
      screenshot(client, acId, label);
    } finally {
      mct(client, 'input', 'key', 'press', 'f1');
    }
  }

  // Clone filtered blocks into an off-camera probe area to find a natural pile.
  // The source chunks and the photographed pile are never edited.
  let sequence = 0;
  async function naturalPile(rock, biome, dimension, acId) {
    command(client, `execute in minecraft:${dimension} run tp @s 0 90 0`);
    await camera(client, { x: 0, y: 90, z: 0 });
    const marker = `m02-locate-${++sequence}`;
    command(client, `locate biome minecraft:${biome}`);
    command(client, `tellraw @s {"text":"${marker}"}`);
    const history = await waitFor(() => mct(client, 'chat', 'history', '--last', '12'),
      data => JSON.stringify(data).includes(marker));
    const positions = [...JSON.stringify(history).matchAll(/\[(-?\d+),\s*-?\d+,\s*(-?\d+)\]/g)];
    if (!positions.length) throw new Error(`No biome location returned for ${biome}`);
    const location = positions.at(-1);
    const cx = Math.floor(Number(location[1]) / 16) * 16;
    const cz = Math.floor(Number(location[2]) / 16) * 16;
    const probeX = cx + 1024;
    const probeZ = cz + 1024;
    const top = dimension === 'the_nether' ? 127 : 255;
    command(client, `forceload add ${probeX} ${probeZ} ${probeX + 63} ${probeZ + 63}`);
    async function count(box) {
      const token = `m02-probe-${++sequence}:`;
      command(client, `scoreboard players set @s m02probe 0`);
      const [x, y, z, endX, endY, endZ] = box;
      const visible = x === endX && y === endY && z === endZ
        ? `if block ${x} ${y - 1} ${z} minecraft:${rock} if biome ${x} ${y} ${z} minecraft:${biome} if block ${x} ${y + 1} ${z} air if block ${x} ${y + 2} ${z} air `
        : '';
      command(client, `execute ${visible}store result score @s m02probe run clone ${box.join(' ')} ${probeX} 0 ${probeZ} filtered tfcoverdims:loose_${rock} force`);
      command(client, `tellraw @s [{"text":"${token}"},{"score":{"name":"@s","objective":"m02probe"}}]`);
      const reply = await waitFor(() => mct(client, 'chat', 'history', '--last', '8'),
        data => JSON.stringify(data).includes(token));
      const number = JSON.stringify(reply).match(new RegExp(`${token}(\\d+)`));
      if (!number) throw new Error(`No natural pile count returned for ${rock}`);
      return Number(number[1]);
    }
    async function find(box) {
      if (await count(box) === 0) return;
      if (box.slice(0, 3).every((value, axis) => value === box[axis + 3])) return box;
      const axis = [0, 1, 2].sort((a, b) => (box[b + 3] - box[b]) - (box[a + 3] - box[a]))[0];
      const midpoint = Math.floor((box[axis] + box[axis + 3]) / 2);
      const lower = [...box];
      const upper = [...box];
      lower[axis + 3] = midpoint;
      upper[axis] = midpoint + 1;
      return await find(lower) ?? await find(upper);
    }
    try {
      // A biome location can be a lava lake or lack exposed matching rock.
      // Search successive tiles while keeping only 32 chunks force-loaded.
      for (let radius = 0; radius <= 6; radius++) {
        for (let dx = -radius; dx <= radius; dx++) {
          for (let dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) !== radius) continue;
            const x = cx + dx * 64;
            const z = cz + dz * 64;
            command(client, `forceload add ${x} ${z} ${x + 63} ${z + 63}`);
            try {
              await camera(client, { x: x + 32, y: 90, z: z + 32 });
              const loaded = [];
              for (const [tileX, tileZ] of [[x, z], [probeX, probeZ]]) {
                for (let ox = 0; ox < 64; ox += 16) {
                  for (let oz = 0; oz < 64; oz += 16) loaded.push(`if loaded ${tileX + ox} 0 ${tileZ + oz}`);
                }
              }
              const ready = `m02-loaded-${++sequence}`;
              const reply = await waitFor(() => {
                command(client, `execute ${loaded.join(' ')} run tellraw @s {"text":"${ready}"}`);
                return mct(client, 'chat', 'history', '--last', '8');
              }, data => JSON.stringify(data).includes(ready), 120_000);
              if (!JSON.stringify(reply).includes(ready)) throw new Error(`Search chunks did not load near ${biome}`);
              const found = await find([x, 0, z, x + 63, top, z + 63]);
              if (found) {
                const [px, py, pz] = found;
                await capture({ x: px + 0.5, y: py + 0.5, z: pz + 0.5, pitch: 90 },
                  acId, `natural-${biome}-${rock}`);
                return;
              }
            } finally {
              command(client, `forceload remove ${x} ${z} ${x + 63} ${z + 63}`);
            }
          }
        }
      }
      throw new Error(`No naturally generated loose ${rock} near ${biome}`);
    } finally {
      command(client, `fill ${probeX} 0 ${probeZ} ${probeX + 63} ${top} ${probeZ + 63} air`);
      command(client, `forceload remove ${probeX} ${probeZ} ${probeX + 63} ${probeZ + 63}`);
    }
  }

  await naturalPile('netherrack', 'nether_wastes', 'the_nether', 'AC-F002-003');
  await naturalPile('blackstone', 'basalt_deltas', 'the_nether', 'AC-F002-003');
  await naturalPile('end_stone', 'end_highlands', 'the_end', 'AC-F005-001');

  command(client, 'execute in minecraft:overworld run tp @s 0 102 0');
  await camera(client, { x: 0, y: 102, z: 0 });
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'fill -3 100 -3 13 100 9 minecraft:smooth_stone');
  command(client, 'fill -3 101 -3 13 107 9 minecraft:air');
  for (const [row, rock, acId] of [
    [0, 'netherrack', 'AC-F002-003'],
    [1, 'blackstone', 'AC-F002-003'],
    [2, 'end_stone', 'AC-F005-001'],
  ]) {
    for (let count = 1; count <= 4; count++) {
      command(client, `setblock ${count * 3 - 3} 100 ${row * 3} minecraft:${rock}`);
      command(client, `setblock ${count * 3 - 3} 101 ${row * 3} tfcoverdims:loose_${rock}[count=${count}]`);
      await capture({ x: count * 3 - 2.5, y: 101.1, z: row * 3 + 0.5, pitch: 90 },
        acId, `${rock}-count-${count}`);
    }
    await capture({ x: 4.5, y: 103, z: row * 3 - 3.5, pitch: 38 },
      acId, `${rock}-one-through-four`);
  }
});
