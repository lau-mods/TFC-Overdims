const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamerule announceAdvancements false');
  command(client, 'recipe give @s *');
  command(client, 'gamemode survival');
  command(client, 'fill -3 100 -3 3 100 3 minecraft:stone');
  command(client, 'setblock 0 101 1 minecraft:enchanting_table');
  await camera(client, { x: 0.5, y: 101, z: -1.5, yaw: 0, pitch: 15 }, 12_000);

  async function openTable() {
    mct(client, 'input', 'key', 'down', 'f3');
    mct(client, 'input', 'key', 'press', 'd');
    mct(client, 'input', 'key', 'up', 'f3');
    mct(client, 'block', 'interact', '0', '101', '1');
    const opened = await waitFor(() => mct(client, 'gui', 'info'), data => data.open === true);
    if (!opened.open) throw new Error('Enchantment table did not open');
  }

  for (const [item, label] of [
    ['minecraft:lapis_lazuli', 'vanilla-lapis'],
    ['tfc:gem/lapis_lazuli', 'tfc-lapis'],
  ]) {
    command(client, 'clear @s');
    command(client, 'experience set @s 30 levels');
    command(client, 'item replace entity @s hotbar.0 with minecraft:iron_sword');
    command(client, `item replace entity @s hotbar.1 with ${item} 3`);
    await openTable();
    mct(client, 'gui', 'click', '29');
    mct(client, 'gui', 'click', '0');
    mct(client, 'gui', 'click', '30');
    mct(client, 'gui', 'click', '1');
    mct(client, 'wait', '15');
    screenshot(client, 'AC-F013-001', `${label}-input`);
    mct(client, 'enchant', '--option', '0');
    mct(client, 'wait', '12');
    screenshot(client, 'AC-F013-001', `${label}-enchanted`);
    mct(client, 'gui', 'close');

    command(client, 'clear @s');
    command(client, `item replace entity @s hotbar.1 with ${item} 3`);
    await openTable();
    mct(client, 'gui', 'click', '30', '--button', 'shift-left');
    mct(client, 'wait', '15');
    screenshot(client, 'AC-F013-002', `${label}-shift-click`);
    mct(client, 'gui', 'close');
  }
});
