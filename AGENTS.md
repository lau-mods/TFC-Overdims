# TFC Overdims 開発ガイド

製品要求と Acceptance Criteria の正本は `PROJECT.md`。計画は
`.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
現在のコードは M01 の Nether / End 気候モデルと GameTest を実装している。
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
