# TFC Overdims 開発ガイド

製品要求と Acceptance Criteria の正本は `PROJECT.md`。計画は
`.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
現在のコードは M01 の Nether / End 気候モデル、M02 の岩石加工と地表小石、
M03 の崩落と Basalt 名称、M04 の Nether 木材、M05 の Calendar 成長、
および GameTest / E2E シナリオを実装している。
他の milestone の製品機能は実装済みとは扱わない。

## M01 気候と構成

- 基本パッケージは `net.claustra01.tfcoverdims`、エントリは `TFCOverdims`。
  `.climate` にモデルと DeferredRegister、`.gametest` に検証を置く。
  クライアント専用クラスは参照しない。
- 気候モデル型 ID は `tfcoverdims:nether` と `tfcoverdims:end`。
  TFC の `SelectClimateModelEvent` で Nether / End にだけ割り当て、
  Overworld のモデル選択・計算には介入しない。
- Nether の基準気温は次の表に従う。未掲載の Nether バイオームは 70 ℃。

  | バイオーム ID | 基準気温 (℃) |
  | --- | --- |
  | `minecraft:nether_wastes` | 70 |
  | `minecraft:basalt_deltas` | 80 |
  | `minecraft:crimson_forest` | 60 |
  | `minecraft:warped_forest` | 60 |
  | `minecraft:soul_sand_valley` | 55 |

- Nether の平均・瞬間気温はともに基準気温から
  `Mth.clampedMap(Y, 32, 128, -5, 5)` ℃を引く。平均・瞬間降水量は 0 mm。
  End は全バイオーム・高度で平均 20 ℃、瞬間 20 ℃、降水量 0 mm。
  両モデルとも季節変動はなく、TFC の既存 Calendar を全 Dimension で共用する。
- TFC 4.2.12 は Modrinth Maven から `compileOnly` と `localRuntime` で参照する。
  mod metadata では `tfc` を required、`[4.2.12,4.3)`、ordering AFTER とする。
  Patchouli 1.21.1-93-neoforge は TFC の必須依存として `localRuntime` に置き、
  本 MOD の直接必須依存には追加しない。Beneath には依存しない。
- `./gradlew runGameTestServer` は `@GameTestHolder("tfcoverdims")` のテストを実行する。
  `tfcoverdims.gametest.report=gametest-results.xml` を mod 独自の `@BeforeBatch` で読み、
  `JUnitLikeTestReporter` が `run/gametest-results.xml` を出力する。
  `tests/acceptance.json` は両 AC を対応付ける。
  `@PrefixGameTestTemplate(false)` により testcase の classname は `tfcoverdims:empty`、
  name は `netherclimate`、`endclimate`、`overworldunchanged` となる。
  Nether 5 バイオーム・高度補正、End 5 バイオーム、Calendar 進行後の不変性、
  Overworld 不変性とモデル型の通信 codec を検証する。M01 に表示要件はない。

## M02 岩石と加工

- `tfcoverdims:loose_netherrack`、`tfcoverdims:loose_blackstone`、
  `tfcoverdims:loose_end_stone` を Block / BlockItem として登録する。
  Item のみの追加は `tfcoverdims:blackstone_brick` と `tfcoverdims:end_stone_brick`。
  Vanilla の岩石・加工ブロック ID を維持し、独自の石器・tool head は登録しない。
- `.common.blocks.LooseRockBlock` は TFC の `GroundcoverBlock` を継承する。
  TFC の `LooseRockBlock` は COUNT が 1～3 のため、独自の COUNT 1～4 を使い、
  同じ小石で右クリックすると1個増加、空の手などで回収すると全個数を返す。
  破壊・支持ブロック消失時の loot も COUNT と同数。流体処理と回収は基底に従う。
- Netherrack / Blackstone / End Stone の block loot のみを上書きする。
  `tfc:is_isolated` なら元ブロック1個、それ以外は対応する Loose Rock を uniform 2～4個。
  3種を `tfc:breaks_when_isolated` に追加して TFC の孤立検出を使う。
  Nether Gold Ore、Nether Quartz Ore、Ancient Debris、Gilded Blackstone は変更しない。
- 各 Loose Rock 4個の2×2から元ブロック1個、元ブロック1個から Loose Rock 4個。
  Loose Rock 1個 + `#c:tools/chisel` から対応する Brick 1個
  （Netherrack は `minecraft:nether_brick`）を作る。
  Brick 4個 + `tfc:mortar` 4個の `BMB / M M / BMB` から対応する
  Nether Bricks / Polished Blackstone Bricks / End Stone Bricks を4個作る。
  工具加工は TFC の advanced shapeless recipe と damage crafting remainder を使う。
  Polished Blackstone Bricks 1個 + `#c:tools/hammer` から cracked variant 1個、
  Blackstone Brick 1個 / 2個 + Chisel から既存 Button / Pressure Plate 各1個。
- `neoforge:false` で無効にする Vanilla recipe ID は `nether_brick`、`nether_bricks`、
  `polished_blackstone_bricks`、`polished_blackstone_bricks_from_blackstone_stonecutting`、
  `polished_blackstone_bricks_from_polished_blackstone_stonecutting`、
  `cracked_polished_blackstone_bricks`、`polished_blackstone_button`、
  `polished_blackstone_pressure_plate`、`end_stone_bricks`、
  `end_stone_bricks_from_end_stone_stonecutting`。
  Polished Blackstone 自体の製作・石切りと階段・塀・ハーフの派生レシピは維持する。
- 各 `loose_*` configured feature は `tfc:random_property` で COUNT 1～4 を選ぶ。
  placed feature は空気かつ下面が対応する Vanilla 岩石で生存可能な場合のみ配置する。
  `loose_*_patch` は random_patch の tries=16、xz_spread=10、y_spread=1。
  placed patch は rarity=3、count_on_every_layer が Netherrack=5、Blackstone=2、
  End Stone=5、biome filter の順。`neoforge:add_features` の vegetal_decoration で
  Nether 2種は `#minecraft:is_nether`、End Stone は `#minecraft:is_end` を対象とする。
  地形・鉱脈 feature は変更しない。
- Loose Blackstone を `tfc:stones/loose/metamorphic`、Loose End Stone を
  `tfc:stones/loose/sedimentary` に追加し、両方を `tfc:rock_knapping` に追加して既存石器レシピを使う。
  Loose Rock の Item Size / Weight は small / light、Brick は very_small / very_light。
  5アイテムそれぞれに item_size を1件設定する。名称は en_us / ja_jp に追加する。
- GameTest は M01 と同じ batch / report / classname を使用する。
  `tests/acceptance.json` の対応は F002-001=`netherrackloot`、F002-002=`blackstoneloot`、
  F002-003=`netherrockplacement` / `rockresources` / `rockitemsizes`、
  F003-001=`netherbrickprocessing`、F003-002=`netherrackreconstruction`、
  F004-001=`blackstonereconstruction`、F004-002=`blackstonebrickprocessing`、
  F004-003=`blackstonederivatives`、F004-004=`blackstoneknapping`、
  F005-001=`endstoneloot` / `endrockplacement` / `rockresources` / `rockitemsizes`、
  F005-002=`endstoneprocessing`、F005-003=`endstoneknapping`。
  複数固定シードの loot、回収、生成設定、工具耐久・材料消費、旧レシピ無効、
  既存派生材・石器、登録アイテム集合、Size / Weight、JSON・モデル・タグ参照を検証する。
  E2E は `tests/e2e/loose-rocks.mjs` を F002-003 / F005-001 に対応させ、
  自然生成地点と各 COUNT の描画を撮影する。

## M03 崩落と Basalt 名称

- `tfc:can_collapse`、`tfc:can_start_collapse`、`tfc:can_trigger_collapse` の
  Block Tag に本 MOD が追加するのは `minecraft:blackstone` と `minecraft:end_stone` のみ。
  `tfcoverdims:collapse/blackstone` と `tfcoverdims:collapse/end_stone` は
  `tfc:collapse` recipe で、それぞれ元と同じ既存ブロックを落下結果にする。
  新規 Cobble は追加せず、Support Beam の保護は TFC 既存処理を使い Java は追加しない。
  Netherrack、Nether Bricks、End Stone Bricks、Vanilla Basalt、鉱石の崩落規則は変更しない。
- 表示名のみを en_us / ja_jp で上書きする。

  | Vanilla ID | en_us | ja_jp |
  | --- | --- | --- |
  | `minecraft:basalt` | Nether Basalt | ネザー玄武岩 |
  | `minecraft:polished_basalt` | Polished Nether Basalt | 磨かれたネザー玄武岩 |
  | `minecraft:smooth_basalt` | Smooth Nether Basalt | 滑らかなネザー玄武岩 |

  TFC の翻訳キーと Registry ID は変更しない。Vanilla Basalt のモデル、テクスチャ、
  loot、recipe、worldgen、rock tag も変更しない。
- GameTest は M01 と同じ batch / report を使う。`collapse` template の
  classname は `tfcoverdims:collapse`。`basaltidentities` は `empty` template を使い、
  classname は `tfcoverdims:empty`。
  `tests/acceptance.json` の対応は F006-001=`blackstonecollapse` / `endstonecollapse`、
  F006-002=`supportbeams` / `collapseexclusions` / `tfcbasaltcollapse`、
  F007-001=`basaltidentities`、F007-002=`basaltgeology` / `tfcbasaltcollapse`。
  採掘からの落下と着地、Beam 保護、対象外ブロックの採掘、既存 TFC Basalt の崩落・採掘、
  Vanilla Basalt の地表小石・岩石 item tag・knapping recipe・鉱石母岩からの分離を検証する。
  英語名はゲームの言語解決と ItemStack の表示名で検証する。
- `tests/e2e/basalt-names.mjs` を F007-001 に対応させ、英語・日本語それぞれで
  Vanilla 3種と TFC Basalt のブロック、表示名、Registry ID、アイテムの tooltip を撮影する。

## M04 Nether 木材

- `.common.wood.OverdimsWood` は `RegistryWood` を実装し、CRIMSON / WARPED のみを追加する。
  Block ID は `tfcoverdims:wood/<type>/<crimson|warped>`、Slab / Stairs は
  `tfcoverdims:wood/planks/<wood>_slab` / `<wood>_stairs`。
  `Wood.BlockType` の全43種（log、stripped_log、wood、stripped_wood、leaves、planks、
  sapling、potted_sapling、bookshelf、door、trapdoor、fence、log_fence、fence_gate、button、
  pressure_plate、slab、stairs、tool_rack、twig、fallen_leaves、vertical_support、
  horizontal_support、workbench、trapped_chest、chest、loom、sluice、sign、wall_sign、barrel、
  lectern、scribing_table、sewing_table、shelf、axle、bladed_axle、encased_axle、clutch、gear_box、
  windmill、water_wheel、crate）を登録する。BlockItem は `needsItem()` に従い、別途
  `wood/lumber/<wood>`、`wood/support/<wood>`、`wood/sign/<wood>` を登録する。
  Vanilla ID は置換せず、Boat、金属 Hanging Sign、独自 BlockEntityType / MenuType は追加しない。
- Sapling の植栽判定以外は `Wood.BlockType.create(wood)` を使う。
  `BlockEntityTypeAddBlocksEvent` で TFC の Chest、Trapped Chest、Crate、Barrel、Loom、Sluice、
  Tool Rack、Bookshelf、Sign（立札・壁札）、Tick Counter（Sapling）、Axle、Bladed Axle、
  Encased Axle、Clutch、Gear Box、Windmill、Water Wheel、Lectern、Shelf に有効ブロックを追加する。
  `registerWoodTypes()` で BlockSetType / WoodType を登録し、Sign の描画登録だけを client 側に置く。
- Sapling は8 TFC日。植栽地は `tfc:tree_grows_on` または TFC 基底の許可する土壌。
  同 tag に両 Nylium を追加し、樹木 Feature と土壌一覧を共有する。
  Vanilla の Crimson / Warped Fungus は `BonemealEvent` の cancel だけで巨大化を抑止し、
  通常の設置・回収は維持する。
- 採掘・葉の維持は `minecraft:mineable/axe`、`mineable/hoe`、Block の `logs` / `leaves`、
  建材・設備は Vanilla / TFC / common の対応 tag、Lumber は `tfc:lumber` に接続する。
  全 BlockType の ExtendedProperties は flammability / spread とも0。
  Item は `minecraft:non_flammable_wood` に入れる。Item の `logs`、`leaves`、`tfc:fallen_leaves`
  は TFC の燃料・Log Pile へ推移的に入るため追加しない。
  `logs_that_burn`、`tfc:firepit_fuel`、`tfc:log_pile_logs` にも追加しない。
  `tfc/support/nether_support_beams` は両 Horizontal Support を up=2、down=2、horizontal=4 で登録する。
- `recipe/wood/` は TFC 4.2.12 の `recipe/crafting/wood/` の oak を両木種へ対応付ける。
  Saw は Log→Lumber 8、Planks→4、Slab→2、Stairs→3、Support は2 Log + Saw→8。
  工具の remainder は TFC の damage crafting remainder を使う。
  Vanilla で無効にする ID は各 `crimson_` / `warped_` に
  `button`、`door`、`fence`、`fence_gate`、`hanging_sign`、`hyphae`、`planks`、`pressure_plate`、
  `sign`、`slab`、`stairs`、`trapdoor` を付けたもの、および `stripped_crimson_hyphae`、
  `stripped_warped_hyphae`。すべて `neoforge:false`。本MODからVanilla木材への変換は追加しない。
- Configured / Placed Feature ID は `tfcoverdims:tree/crimson` と `tfcoverdims:tree/warped`。
  型は `tfc:random_tree`、構造候補16 / 17、幹高3～5、placement は
  `minecraft:count_on_every_layer` の8、次に biome filter。
  `structure/crimson/1..16.nbt`、`warped/1..17.nbt` は Beneath 1.21.x
  revision `eab10327879400aeb67b3f9b3f7e2fbaf60946d0` の同パスを、形状と BlockState を維持して
  `beneath:wood/` → `tfcoverdims:wood/` に remap する。
  `remove_<wood>_fungi` は対応する `minecraft:<wood>_forest` の vegetal_decoration から
  `minecraft:<wood>_fungi` のみ除外し、`add_<wood>_trees` が本MODの樹木を追加する。
  M02 の小石以外の既存 Nether Feature は維持する。
- モデルと木材画像の基準は TFC v4.2.12 の `assets/tfc/{models,textures}/` の oak。
  木色の変換は HSV の H を Crimson=0.94、Warped=0.49 に置き換え、S・V・alpha を保持し、
  RGB を255倍して整数化する。パレット生成モデルは元の atlas の oak 色展開後に同じ変換を行う。
  TFC renderer が固定参照する Chest、Planks、Water Wheel は `assets/tfc/textures/` に配置する。
  その他は `assets/tfcoverdims/`、Scribing Table の共通 `scribing_paraphernalia` は TFC を参照する。
  Sapling、Potted Sapling、Bare Leaves、Door、Trapdoor は cutout、その他の Leaves は cutout_mipped。
  名称は en_us / ja_jp。Size / Weight は TFC oak の同種と一致させ、既存 tag の定義が適用される品は
  それを使い、残りだけ個別 item_size を置いて適用数を1件にする。
- GameTest は既存 climate batch / `run/gametest-results.xml`。
  `tests/acceptance.json` は F008-001=`woodregistry` / `woodstorage` / `woodharvest` / `rockresources`、
  F008-002=`nethertrees`、F008-003=`saplingandfungus`、
  F008-004=`woodprocessing` / `woodstorage` / `woodfuel` / `woodsupportsandmachines`、
  F008-005=`vanillawoodrecipes`。
  classname は `nethertrees` / `saplingandfungus` が `tfcoverdims:nether_tree`（19×32×19）、
  `woodsupportsandmachines` が `tfcoverdims:collapse`、他は `tfcoverdims:empty`。
  全 BlockType と BlockEntity の有効性、収納保存・再読込、oak と同形の全加工レシピ・工具消耗、
  Axe 採掘と葉の維持、Support 保護、Motor→Axle→Gear Box→Clutch→Axle の回転、
  火炎・Fuel lookup・Firepit / Log Pile 拒否、8日成長、実際の骨粉使用と Fungus 回収、
  読み込み済み Feature / Biome と構造ブロック、モデル参照を検証する。
- E2E `tests/e2e/nether-wood.mjs` を F008-001 に対応させる。
  両木種の建材、植物・透明部分・文字入りSign、収納・作業設備、全機械部品を並べ、
  Lumber / Support / Sign の両木種のアイテムを hotbar に表示して撮影する。

## M05 Nether Wart / Chorus の Calendar 成長

- common 側の `NetherWartBlockMixin` / `ChorusFlowerBlockMixin` が Vanilla の
  `randomTick` を TFC Calendar の経過時間へ接続する。`onPlace` で時刻を記録し、
  `onRemove` で成熟（Wart age 3 / Flower age 5）または別ブロックへの置換・除去時に削除する。
  `tfcoverdims.mixins.json` は mod metadata の `[[mixins]]` で有効化する。
- `tfcoverdims:plant_growth` は保存可能な chunk data attachment。
  BlockPos の packed long を文字列キーとし、各位置の最終更新 Calendar tick を保持する。
  既存植物に記録がない場合は最初の random tick 時点から計測する。
- `CalendarGrowth.interval(ICalendar)` は Vanilla の平均 random tick 間隔に相当する
  1366 game ticks を Calendar ticks に変換し、最小値を1とする。
  経過した間隔ごとに Vanilla の成長処理を呼び、Chorus の新しい Flower にも残り時間を適用する。
  1回の random tick の再生上限は4096回。未処理時間と端数は次回へ保持する。
- Soul Sand / End Stone の土台条件、Vanilla の age・形状・分岐・成熟・採取 loot は変更しない。
  新規農地、栄養素、作物、成長段階は追加しない。
- GameTest は既存の climate batch / `run/gametest-results.xml` を使用する。
  `tests/acceptance.json` の F009-001 は `wartcalendar` と `growthpersistence`、
  F009-002 は `choruscalendar` と `growthpersistence` に対応する。
  classname は `choruscalendar` が `tfcoverdims:nether_tree`、残りは `tfcoverdims:empty`。
  Calendar 経過・端数・成長上限・採取・土台条件、Chorus 分岐、attachment の保存・再読込と
  植え直し時の時刻リセットを検証する。表示要件がないため M05 の E2E は登録しない。

## Harness runtime

- Minecraft 1.21.1、Java 21、NeoForge 21.1.264 を使用する。
  NeoForge の現在値は `gradle.properties` の `neo_version` に従う。
- runtime は Git 管理外の `.harness-state/runtime/` に置く。
  MC Pilot CLI 0.20.2 は `pilot/node_modules/.bin/mct`、専用サーバーは `server/`。
  CLI の `data/variants.json` の `1.21.1-neoforge` は 21.1.264 に合わせる。
  クライアント生成が途中で終了する場合は、公式 NeoForge installer の
  `--installClient` で runtime cache を完成させてから再作成する。
- MC Pilot の環境変数は `MCT_HOME=$PWD/.harness-state/runtime/mct-home`、
  `MCT_CACHE_DIR=$PWD/.harness-state/runtime/mct-cache`、`MCT_SKILL_TARGETS=none`。
- 操作用クライアント名は `tfcoverdims-a`。サーバーは
  `127.0.0.1:25579`、検証ワールドは `harness-world`。
  Harness は E2E ごとにこのワールドを再作成する。
- TFC 4.2.12 と必須依存 Patchouli 1.21.1-93-NEOFORGE をサーバーと
  クライアントの `mods/` に配置する。Beneath は実行時依存にしない。
- macOS のクライアントには CPU architecture に合う Java 21 を使用する。
  この環境では Homebrew の `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java`。
- TFC の初回ログインで block cache を再構築するため、サーバーの
  `user_jvm_args.txt` とクライアントの `instance.json` の `env.JAVA_TOOL_OPTIONS` に
  `-Dneoforge.readTimeout=120` を指定する。両者の最大 heap は 3 GiB。
- `npm --prefix .harness run build` で Harness をビルドし、
  `node .harness/dist/cli/main.js doctor` と `validate` で準備を確認する。
  `plan` は Claude CLI の認証が利用できる環境で実行し、計画と進捗を commit する。
- 製品コードを変更した場合の完了確認は `./gradlew build`。
  runtime 準備だけで製品の GameTest / E2E 成功とは扱わない。
