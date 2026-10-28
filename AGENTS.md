# TFC Overdims 開発ガイド

製品要求と Acceptance Criteria の正本は `PROJECT.md`。計画は
`.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
現在のコードは M01 の Nether / End 気候モデルと M02 の岩石加工、地表小石、
GameTest / E2E シナリオを実装している。他の milestone の製品機能は実装済みとは扱わない。

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
