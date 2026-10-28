const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  const blocks = [
    ['minecraft:basalt', 'block.minecraft.basalt'],
    ['minecraft:polished_basalt', 'block.minecraft.polished_basalt'],
    ['minecraft:smooth_basalt', 'block.minecraft.smooth_basalt'],
    ['tfc:rock/raw/basalt', 'block.tfc.rock.raw.basalt'],
  ];
  command(client, 'gamemode creative');
  command(client, 'item replace entity @s hotbar.0 with minecraft:basalt');
  command(client, 'gamerule doMobSpawning false');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  await camera(client, { x: 6.5, y: 102, z: -7, pitch: 5 });
  command(client, 'fill -3 100 -9 16 100 5 minecraft:smooth_stone');
  command(client, 'fill -3 101 -9 16 106 5 minecraft:air');
  command(client, 'setblock 6 101 -4 minecraft:chest');
  for (const [index, [id]] of blocks.entries()) {
    command(client, `setblock ${index * 4} 101 0 ${id}`);
    command(client, `item replace block 6 101 -4 container.${index} with ${id}`);
  }

  async function screen(type) {
    const info = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === type);
    if (info.type !== type) throw new Error(`Expected ${type}, got ${info.type}`);
    return info;
  }

  async function language(index, name) {
    mct(client, 'input', 'key', 'press', 'escape');
    let info = await screen('PauseScreen');
    // NeoForge 1.21.1's pause grid includes the Mods row.
    mct(client, 'input', 'click', String(info.width / 2 - 53), String(Math.floor((info.height - 190) / 4) + 132));
    info = await screen('OptionsScreen');
    mct(client, 'input', 'click', String(info.width / 2 - 79), String(Math.min(91, info.height - 153) + 58));
    info = await screen('LanguageSelectScreen');
    mct(client, 'input', 'scroll', String(info.width / 2), String(info.height / 2), '--delta', '1000');
    mct(client, 'input', 'click', String(info.width / 2), '46');
    // The 1.21.1 language list is sorted by code: en_us=27, ja_jp=70.
    for (let row = 0; row < index; row++) mct(client, 'input', 'key', 'press', 'down');
    mct(client, 'input', 'key', 'press', 'enter');
    await screen('OptionsScreen');
    const item = await waitFor(() => mct(client, 'inventory', 'slot', '0'),
      data => data.item?.displayName === name, 120_000);
    if (item.item?.displayName !== name) throw new Error('Language resource reload did not finish');
    mct(client, 'gui', 'close');
  }

  for (const [code, index, name] of [['en_us', 27, 'Nether Basalt'], ['ja_jp', 70, 'ネザー玄武岩']]) {
    await language(index, name);
    // Recreate labels so cached text layout resolves in the active language.
    command(client, 'kill @e[type=minecraft:text_display,tag=m03-basalt]');
    for (const [index, [id, key]] of blocks.entries()) {
      const text = JSON.stringify([{ translate: key }, { text: `\n${id}`, color: 'gray' }]);
      command(client, `summon minecraft:text_display ${index * 4 + 0.5} 103 0.5 {Tags:["m03-basalt"],billboard:"center",line_width:240,shadow:1b,transformation:{scale:[0.6f,0.6f,0.6f]},text:${JSON.stringify(text)}}`);
    }
    await camera(client, { x: 6.5, y: 102, z: -7, pitch: 5 }, 6_000);
    screenshot(client, 'AC-F007-001', `${code}-blocks-and-registry-ids`);
    mct(client, 'block', 'interact', '6', '101', '-4');
    await screen('ContainerScreen');
    const layout = mct(client, 'gui', 'layout');
    for (let slot = 0; slot < blocks.length; slot++) {
      const position = layout.slots.find(entry => entry.slot === slot);
      mct(client, 'input', 'mouse-move', String(position.centerX), String(position.centerY));
      screenshot(client, 'AC-F007-001', `${code}-item-${slot}`);
    }
    mct(client, 'gui', 'close');
  }
  await language(27, 'Nether Basalt');
});
