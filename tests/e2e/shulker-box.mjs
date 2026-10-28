const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamemode creative');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  command(client, 'execute in minecraft:the_end run tp @s 1024.5 101 -5');
  command(client, 'fill 1019 100 -8 1029 100 5 minecraft:end_stone');
  command(client, 'fill 1019 101 -8 1029 105 5 minecraft:air');
  command(client, 'setblock 1022 101 1 minecraft:shulker_box');
  command(client, 'setblock 1024 101 1 minecraft:blue_shulker_box');
  command(client, 'setblock 1026 101 1 minecraft:red_shulker_box');
  command(client, 'item replace block 1024 101 1 container.0 with tfc:metal/ingot/gold 4');
  await camera(client, { x: 1024.5, y: 101.6, z: -3, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F014-002', 'vanilla-shulker-colors-and-models');
  mct(client, 'block', 'interact', '1024', '101', '1');
  const gui = await waitFor(() => mct(client, 'gui', 'info'), data => data.open === true);
  if (!gui.open) throw new Error('Shulker box did not open');
  screenshot(client, 'AC-F014-002', 'blue-box-with-tfc-ingots');
});
