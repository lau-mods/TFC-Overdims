# TFC Overdims 開発ガイド

製品要求と Acceptance Criteria の正本は `PROJECT.md`。計画は
`.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
現在のコードは MDK の雛形であり、製品機能の実装済みとは扱わない。

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
