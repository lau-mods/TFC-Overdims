const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamemode creative');
  command(client, 'gamerule doMobSpawning false');
  command(client, 'gamerule randomTickSpeed 0');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  await camera(client, { x: 5.5, y: 104, z: -9.5, yaw: 0, pitch: 17 });
  command(client, 'setblock 5 103 -10 minecraft:barrier');
  command(client, 'fill -3 100 -4 14 100 6 minecraft:smooth_stone');
  command(client, 'fill -3 101 -4 14 107 6 minecraft:air');
  const rocks = ['netherrack', 'blackstone', 'end_stone'];
  for (const [index, rock] of rocks.entries()) {
    for (let count = 1; count <= 4; count++) {
      const x = index * 5 + count - 2;
      command(client, `setblock ${x} 100 1 minecraft:${rock}`);
      command(client, `setblock ${x} 101 1 tfcoverdims:loose_${rock}[count=${count}]`);
    }
  }
  for (const [index, wood] of ['crimson', 'warped'].entries()) {
    const x = index * 7 + 2;
    const blocks = ['planks', 'vertical_support', 'workbench', 'crate', 'door',
      'trapdoor', 'leaves', 'chest', 'barrel', 'axle', 'gear_box'];
    for (const [slot, block] of blocks.entries()) {
      command(client, `setblock ${x + slot % 6} 101 ${3 + Math.floor(slot / 6) * 2} tfcoverdims:wood/${block}/${wood}`);
    }
    command(client, `setblock ${x} 100 2 minecraft:${wood}_nylium`);
    command(client, `setblock ${x} 101 2 tfcoverdims:wood/sapling/${wood}`);
    command(client, `setblock ${x + 1} 101 2 tfcoverdims:wood/sign/${wood}`);
  }
  command(client, 'setblock 0 101 -2 minecraft:nether_bricks');
  command(client, 'setblock 1 101 -2 minecraft:polished_blackstone_bricks');
  command(client, 'setblock 2 101 -2 minecraft:end_stone_bricks');
  command(client, 'setblock 4 101 -2 tfcoverdims:cobble_netherrack');
  command(client, 'setblock 5 101 -2 tfcoverdims:hardened_netherrack');
  command(client, 'setblock 6 101 -2 tfcoverdims:cobble_end_stone');
  command(client, 'setblock 7 101 -2 tfcoverdims:hardened_end_stone');
  command(client, 'setblock 5 101 -1 minecraft:chest');
  const items = ['loose_netherrack', 'loose_blackstone', 'loose_end_stone', 'blackstone_brick', 'end_stone_brick',
    'wood/lumber/crimson', 'wood/support/crimson', 'wood/lumber/warped', 'wood/support/warped'];
  for (const [slot, id] of items.entries()) command(client, `item replace block 5 101 -1 container.${slot} with tfcoverdims:${id}`);
  command(client, 'item replace entity @s hotbar.0 with tfcoverdims:loose_netherrack');
  command(client, 'item replace entity @s hotbar.1 with minecraft:chest');
  command(client, 'item replace entity @s hotbar.2 with tfcoverdims:wood/support/crimson');

  async function screen(type) {
    const info = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === type);
    if (info.type !== type) throw new Error(`Expected ${type}, got ${info.type}`);
    return info;
  }

  async function captureBlocks(position, label) {
    command(client, `setblock ${Math.floor(position.x)} ${position.y - 1} ${Math.floor(position.z)} minecraft:barrier`);
    await camera(client, position, 10_000);
    mct(client, 'look', 'set', '--yaw', String(position.yaw), '--pitch', String(position.pitch));
    const matches = actual => ['x', 'y', 'z', 'yaw', 'pitch']
      .every(key => Math.abs(actual[key] - position[key]) < 0.1);
    const actual = await waitFor(() => ({
      ...mct(client, 'position', 'get'),
      ...mct(client, 'rotation', 'get'),
    }), matches);
    if (!matches(actual)) throw new Error(`Camera did not reach ${label}: ${JSON.stringify(actual)}`);
    screenshot(client, 'AC-F016-001', label);
  }

  async function language(index, name, chestName) {
    const translated = () => mct(client, 'inventory', 'slot', '0').item?.displayName === name
      && mct(client, 'inventory', 'slot', '1').item?.displayName === chestName;
    if (translated()) return;
    if (mct(client, 'gui', 'info').open) mct(client, 'gui', 'close');
    let info;
    for (let attempt = 0; attempt < 3; attempt++) {
      mct(client, 'input', 'key', 'press', 'escape');
      info = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === 'PauseScreen', 3_000);
      if (info.type === 'PauseScreen') break;
    }
    if (info.type !== 'PauseScreen') throw new Error(`Expected PauseScreen, got ${info.type}`);
    mct(client, 'input', 'click', String(info.width / 2 - 53), String(Math.floor((info.height - 190) / 4) + 132));
    info = await screen('OptionsScreen');
    for (const y of [119, 131, 143, 155, 167, 179, 191, 107, 95]) {
      mct(client, 'input', 'click', String(info.width / 2 - 79), String(y));
      info = mct(client, 'gui', 'info');
      if (info.type === 'LanguageSelectScreen') break;
      if (info.type !== 'OptionsScreen') {
        for (let back = 0; back < 3 && info.type !== 'OptionsScreen'; back++) {
          mct(client, 'input', 'key', 'press', 'escape');
          info = mct(client, 'gui', 'info');
        }
        if (info.type !== 'OptionsScreen') throw new Error(`Cannot return to OptionsScreen from ${info.type}`);
      }
    }
    if (info.type !== 'LanguageSelectScreen') throw new Error('Language button not found');
    mct(client, 'input', 'scroll', String(info.width / 2), String(info.height / 2), '--delta', '1000');
    mct(client, 'input', 'click', String(info.width / 2), '46');
    for (let row = 0; row < index; row++) mct(client, 'input', 'key', 'press', 'down');
    mct(client, 'input', 'key', 'press', 'enter');
    await screen('OptionsScreen');
    const names = await waitFor(() => ({
      mod: mct(client, 'inventory', 'slot', '0').item?.displayName,
      chest: mct(client, 'inventory', 'slot', '1').item?.displayName,
    }), data => data.mod === name && data.chest === chestName, 120_000);
    if (names.mod !== name || names.chest !== chestName) {
      throw new Error(`Wrong client language: ${JSON.stringify(names)}`);
    }
    mct(client, 'gui', 'close');
    await waitFor(() => mct(client, 'gui', 'info'), data => data.open === false);
  }

  for (const [code, index, name, chestName] of [
    ['en_us', 27, 'Loose Netherrack', 'Chest'],
    ['ja_jp', 70, 'ネザーラックの小石', 'チェスト'],
  ]) {
    await language(index, name, chestName);
    const pebble = await waitFor(() => mct(client, 'block', 'get', '5', '101', '1'),
      data => data.type === 'tfcoverdims:loose_blackstone', 60_000);
    if (pebble.type !== 'tfcoverdims:loose_blackstone') throw new Error('Rock layout is not loaded');
    const wood = mct(client, 'block', 'get', '2', '101', '3');
    if (wood.type !== 'tfcoverdims:wood/planks/crimson') throw new Error('Wood layout is not loaded');
    await captureBlocks({ x: 5.5, y: 104, z: -8.5, yaw: 0, pitch: 22 }, `${code}-rocks`);
    await captureBlocks({ x: 7.5, y: 104, z: -5.5, yaw: 0, pitch: 22 }, `${code}-wood`);
    await camera(client, { x: 5.5, y: 101, z: -2.5, yaw: 0, pitch: 20 });
    mct(client, 'block', 'interact', '5', '101', '-1');
    await screen('ContainerScreen');
    const layout = mct(client, 'gui', 'layout');
    mct(client, 'input', 'mouse-move', '10', '10');
    screenshot(client, 'AC-F016-001', `${code}-items`);
    const support = layout.slots.find(slot => slot.slot === 56);
    if (!support) throw new Error('Support hotbar slot is missing');
    mct(client, 'input', 'mouse-move', String(support.centerX), String(support.centerY));
    screenshot(client, 'AC-F016-001', `${code}-support-tooltip`);
    mct(client, 'gui', 'close');
  }
  await language(27, 'Loose Netherrack', 'Chest');
});
