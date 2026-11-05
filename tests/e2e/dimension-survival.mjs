import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';

const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamerule announceAdvancements false');
  command(client, 'recipe give @s minecraft:popped_chorus_fruit');
  command(client, 'gamemode creative');
  command(client, 'gamerule doMobSpawning false');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  command(client, 'execute in minecraft:the_end run tp @s 1024.5 101 0.5');
  const world = await waitFor(() => mct(client, 'status', 'world'), data => data.dimension === 'minecraft:the_end');
  if (world.dimension !== 'minecraft:the_end') throw new Error('Food scene requires the End');
  const arrivalGui = await waitFor(() => mct(client, 'gui', 'info'), data => data.open === false);
  if (arrivalGui.open !== false) throw new Error('Dimension loading screen did not close');

  // Keep the food scene outside the dragon's central-island flight area.
  await camera(client, { x: 1024.5, y: 101, z: 0.5 });
  command(client, 'fill 1012 100 -12 1036 100 12 minecraft:end_stone');
  command(client, 'fill 1012 101 -12 1036 110 12 minecraft:barrier');
  command(client, 'fill 1012 101 -12 1036 110 12 minecraft:air');
  command(client, 'setblock 1024 100 0 minecraft:purple_concrete');
  command(client, 'setblock 1024 101 3 minecraft:chest');
  command(client, 'item replace block 1024 101 3 container.0 with minecraft:chorus_fruit');
  await camera(client, { x: 1024.5, y: 101, z: 0.5, pitch: 10 });
  mct(client, 'block', 'interact', '1024', '101', '3');
  const gui = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === 'ContainerScreen');
  if (gui.type !== 'ContainerScreen') throw new Error('Food display chest did not open');
  const layout = mct(client, 'gui', 'layout');
  const slot = layout.slots.find(entry => entry.slot === 0);
  mct(client, 'input', 'mouse-move', String(slot.centerX), String(slot.centerY));
  // MC Pilot's synthetic key callbacks do not update GLFW's polled modifier state.
  const instance = mct(client, 'client', 'list').clients.find(entry => entry.name === client);
  const shift = spawn('java', ['-Djava.awt.headless=false', '-Dapple.awt.UIElement=true',
    fileURLToPath(new URL('./HoldShift.java', import.meta.url)), String(instance.pid)],
    { stdio: ['pipe', 'pipe', 'inherit'] });
  const exited = once(shift, 'exit');
  try {
    const ready = await Promise.race([
      once(shift.stdout, 'data').then(([data]) => data.toString().trim()),
      exited.then(() => ''),
    ]);
    if (ready !== 'ready') throw new Error('Native Shift input did not start');
    // Native modifier events can restore the physical cursor position.
    mct(client, 'input', 'mouse-move', String(slot.centerX), String(slot.centerY));
    await new Promise(resolve => setTimeout(resolve, 1_000));
    screenshot(client, 'AC-F010-002', 'chorus-tfc-food-tooltip');
  } finally {
    if (shift.exitCode === null) shift.stdin.end('\n');
    await exited;
  }
  mct(client, 'gui', 'close');
  command(client, 'gamemode survival');
  command(client, 'tfc player @s set hunger 18');
  command(client, 'item replace entity @s hotbar.0 with minecraft:chorus_fruit 2');
  mct(client, 'inventory', 'hotbar', '0');
  await camera(client, { x: 1024.5, y: 101, z: 0.5, yaw: 180, pitch: 65 }, 12_000);
  screenshot(client, 'AC-F010-002', 'before-eating-on-purple-marker');
  const before = mct(client, 'position', 'get');
  mct(client, 'input', 'key', 'down', 'use');
  try {
    const consumed = await waitFor(() => mct(client, 'inventory', 'slot', '0'), data => data.item?.count === 1);
    if (consumed.item?.count !== 1) throw new Error('Chorus fruit was not consumed');
  } finally {
    mct(client, 'input', 'key', 'up', 'use');
  }
  const after = mct(client, 'position', 'get');
  if (after.x === before.x && after.y === before.y && after.z === before.z) throw new Error('Chorus did not teleport');
  screenshot(client, 'AC-F010-002', 'after-vanilla-chorus-teleport');
});
