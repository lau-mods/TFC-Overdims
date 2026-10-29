const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamemode creative');
  mct(client, 'gui', 'close');
  command(client, 'clear @s');

  async function capture(position, label) {
    // Keep the creative player at camera height while chunks finish rendering.
    command(client, `setblock ${Math.floor(position.x)} ${position.y - 1} ${Math.floor(position.z)} minecraft:barrier`);
    await camera(client, position, 10_000);
    mct(client, 'look', 'set', '--yaw', String(position.yaw), '--pitch', String(position.pitch));
    const matches = data => ['x', 'y', 'z', 'yaw', 'pitch'].every(key => Math.abs(data[key] - position[key]) < 0.1);
    const actual = await waitFor(() => ({
      ...mct(client, 'position', 'get'),
      ...mct(client, 'rotation', 'get'),
    }), matches);
    if (!matches(actual)) throw new Error(`Camera did not settle for ${label}: ${JSON.stringify(actual)}`);
    // Feedback is disabled, so waiting also lets any earlier chat messages fade.
    await new Promise(resolve => setTimeout(resolve, 11_000));
    screenshot(client, 'AC-F008-001', label);
  }

  command(client, 'gamerule doMobSpawning false');
  command(client, 'gamerule randomTickSpeed 0');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'effect give @s minecraft:night_vision infinite 0 true');

  const panels = [
    ['construction', ['log', 'wood', 'stripped_log', 'stripped_wood', 'planks', 'planks_slab',
      'planks_stairs', 'fence', 'log_fence', 'fence_gate', 'vertical_support', 'horizontal_support']],
    ['plants-and-cutouts', ['sapling', 'potted_sapling', 'leaves', 'twig', 'fallen_leaves',
      'door', 'trapdoor', 'button', 'pressure_plate', 'sign', 'wall_sign']],
    ['equipment', ['chest', 'trapped_chest', 'barrel', 'crate', 'tool_rack', 'workbench',
      'loom', 'sluice', 'scribing_table', 'sewing_table', 'bookshelf', 'lectern', 'shelf']],
    ['mechanical-parts', ['axle', 'bladed_axle', 'encased_axle', 'gear_box', 'clutch']],
    ['large-mechanisms', ['windmill', 'water_wheel']],
  ];
  for (const [panel, [label, types]] of panels.entries()) {
    const origin = panel * 48;
    await camera(client, { x: origin + 10, y: 106, z: -12, yaw: 0, pitch: 20 });
    command(client, `fill ${origin - 3} 100 -4 ${origin + 26} 100 24 minecraft:smooth_stone`);
    for (const [index, type] of types.entries()) {
      const x = origin + (index % 4) * 6;
      const z = label === 'large-mechanisms' ? index * 16 : Math.floor(index / 4) * 6;
      for (const [species, wood] of ['crimson', 'warped'].entries()) {
        const px = label === 'large-mechanisms' ? origin + species * 16 : x + species * 2;
        const y = type === 'water_wheel' || type === 'windmill' ? 104 : 101;
        let path = `${type}/${wood}`;
        if (type === 'planks_slab') path = `planks/${wood}_slab`;
        if (type === 'planks_stairs') path = `planks/${wood}_stairs`;
        let state = '';
        let data = '';
        if (type === 'sapling') command(client, `setblock ${px} 100 ${z} minecraft:${wood}_nylium`);
        if (type === 'leaves') state = '[persistent=true]';
        if (type === 'sluice') state = '[facing=north,upper=true]';
        if (type === 'door') state = '[half=lower,facing=north]';
        if (type === 'trapdoor') state = '[facing=north,open=true]';
        if (['tool_rack', 'shelf', 'wall_sign', 'button'].includes(type)) {
          command(client, `setblock ${px} ${y} ${z + 1} minecraft:smooth_stone`);
          state = '[facing=north]';
        }
        if (type === 'horizontal_support') {
          for (const dx of [-1, 1]) command(client, `setblock ${px + dx} ${y} ${z} tfcoverdims:wood/vertical_support/${wood}`);
          state = '[east=true,west=true]';
        }
        if (type === 'sign') state = '[rotation=8]';
        if (type === 'sign' || type === 'wall_sign') {
          data = `{front_text:{messages:['{"text":"${wood}"}','{"text":"TFC wood"}','{"text":""}','{"text":""}']}}`;
        }
        if (['axle', 'bladed_axle', 'encased_axle', 'clutch', 'windmill', 'water_wheel'].includes(type)) state = '[axis=z]';
        if (type === 'windmill') {
          data = '{inventory:{Size:5,Items:[{Slot:0,id:"tfc:windmill_blade/white",count:1},{Slot:1,id:"tfc:windmill_blade/white",count:1},{Slot:2,id:"tfc:windmill_blade/white",count:1},{Slot:3,id:"tfc:windmill_blade/white",count:1}]}}';
        }
        command(client, `setblock ${px} ${y} ${z} tfcoverdims:wood/${path}${state}${data}`);
        if (type === 'sluice') command(client, `setblock ${px} ${y} ${z - 1} tfcoverdims:wood/sluice/${wood}[facing=north,upper=false]`);
        if (type === 'door') command(client, `setblock ${px} ${y + 1} ${z} tfcoverdims:wood/door/${wood}[half=upper,facing=north]`);
      }
    }
    for (const species of [0, 1]) {
      const x = origin + (label === 'large-mechanisms' ? species * 16 : species * 2);
      const y = label === 'large-mechanisms' ? 104 : 101;
      const wood = ['crimson', 'warped'][species];
      const expected = `tfcoverdims:wood/${types[0]}/${wood}`;
      const block = await waitFor(() => mct(client, 'block', 'get', String(x), String(y), '0'),
        data => data.type === expected, 60_000);
      if (block.type !== expected) throw new Error(`Panel not received by client: ${label} ${wood}`);
    }
    await capture(label === 'large-mechanisms'
      ? { x: origin + 8.5, y: 114, z: -17.5, yaw: 0, pitch: 24 }
      : { x: origin + 10.5, y: 108, z: -11.5, yaw: 0, pitch: 22 }, label);
  }

  command(client, 'clear @s');
  for (const [species, wood] of ['crimson', 'warped'].entries()) {
    for (const [index, kind] of ['lumber', 'support', 'sign'].entries()) {
      command(client, `item replace entity @s hotbar.${species * 3 + index} with tfcoverdims:wood/${kind}/${wood}`);
    }
  }
  for (const [slot, wood] of [[1, 'crimson'], [4, 'warped']]) {
    const expected = `tfcoverdims:wood/support/${wood}`;
    const item = await waitFor(() => mct(client, 'inventory', 'slot', String(slot)),
      data => data.item?.type === expected);
    if (item.item?.type !== expected) throw new Error(`Expected hotbar item: ${expected}`);
  }
  await capture({ x: 10.5, y: 108, z: -11.5, yaw: 0, pitch: 22 }, 'lumber-support-sign-items');

  command(client, 'clear @s');
  const chestItems = ['chest/crimson', 'trapped_chest/crimson', 'chest/warped', 'trapped_chest/warped'];
  for (const [slot, path] of chestItems.entries()) {
    const expected = `tfcoverdims:wood/${path}`;
    command(client, `item replace entity @s hotbar.${slot} with ${expected}`);
    const item = await waitFor(() => mct(client, 'inventory', 'slot', String(slot)),
      data => data.item?.type === expected);
    if (item.item?.type !== expected) throw new Error(`Expected chest item: ${expected}`);
  }
  await capture({ x: 10.5, y: 108, z: -11.5, yaw: 0, pitch: 22 }, 'chest-items');
});
