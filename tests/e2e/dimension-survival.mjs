import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';

const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  async function captureArrival(label) {
    const gui = await waitFor(() => mct(client, 'gui', 'info'), data => data.open === false);
    if (gui.open !== false) throw new Error('Dimension loading screen did not close');
    const position = mct(client, 'position', 'get');
    await camera(client, position, 12_000);
    screenshot(client, 'AC-F015-001', label);
  }

  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamerule announceAdvancements false');
  command(client, 'recipe give @s minecraft:popped_chorus_fruit');
  command(client, 'gamemode creative');
  command(client, 'gamerule doMobSpawning false');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');
  command(client, 'execute in minecraft:overworld run tp @s 0.5 101 -3');
  command(client, 'fill -10 100 -10 25 100 12 minecraft:stone');
  command(client, 'fill -10 101 -10 25 108 12 minecraft:barrier');
  command(client, 'fill -10 101 -10 25 108 12 minecraft:air');
  command(client, 'fill 0 100 0 3 104 0 minecraft:obsidian');
  command(client, 'fill 1 101 0 2 103 0 minecraft:air');
  command(client, 'item replace entity @s hotbar.0 with minecraft:flint_and_steel');
  mct(client, 'inventory', 'hotbar', '0');
  await camera(client, { x: 1.5, y: 101, z: -2, pitch: 35 });
  mct(client, 'block', 'place', '1', '101', '0', '--face', 'up');
  const portal = await waitFor(() => mct(client, 'block', 'get', '1', '101', '0'),
    data => data.type === 'minecraft:nether_portal');
  if (portal.type !== 'minecraft:nether_portal') throw new Error('Obsidian frame did not ignite');
  await camera(client, { x: 1.5, y: 101, z: -4, pitch: 0 }, 12_000);
  screenshot(client, 'AC-F015-001', 'obsidian-frame-lit-with-flint-and-steel');
  command(client, 'tp @s 1.5 101 0.5');
  let world = await waitFor(() => mct(client, 'status', 'world'), data => data.dimension === 'minecraft:the_nether');
  if (world.dimension !== 'minecraft:the_nether') throw new Error('Nether portal travel failed');
  await captureArrival('nether-arrival');

  command(client, 'execute in minecraft:overworld run tp @s 12.5 101 -3');
  const frames = [];
  for (let offset = -1; offset <= 1; offset++) {
    frames.push([12 + offset, -2, 'south'], [12 + offset, 2, 'north'],
      [10, offset, 'east'], [14, offset, 'west']);
  }
  for (const [x, z, facing] of frames) {
    command(client, `setblock ${x} 101 ${z} minecraft:end_portal_frame[facing=${facing},eye=false]`);
  }
  command(client, 'item replace entity @s hotbar.0 with minecraft:ender_eye 12');
  for (const [x, z] of frames) mct(client, 'block', 'interact', String(x), '101', String(z));
  await camera(client, { x: 12.5, y: 104, z: -5, pitch: 45 }, 12_000);
  screenshot(client, 'AC-F015-001', 'end-frame-opened-with-eyes');
  command(client, 'tp @s 12.5 101 0.5');
  world = await waitFor(() => mct(client, 'status', 'world'), data => data.dimension === 'minecraft:the_end');
  if (world.dimension !== 'minecraft:the_end') throw new Error('End portal travel failed');
  await captureArrival('end-arrival');

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
