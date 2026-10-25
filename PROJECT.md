# Project

Status: draft

Project ID: tfc-overdims
Mod ID: tfcoverdims

## Platform

Minecraft: 1.21.1
NeoForge: 21.1.262
Java: 21

## Purpose

TFC Overdims は、Vanilla の Nether と End に存在するブロック、植物、木材、食料、Mob、構造物報酬を TerraFirmaCraft（以下 TFC）の既存の加工・生存・装備・収納システムへ接続する NeoForge アドオンである。Dimension とその固有報酬を維持し、Beneath 独自の資源、装置、儀式、Mob、作物および進行システムは追加しない。

コードの基本パッケージは `net.claustra01.tfcoverdims`、登録名前空間は `tfcoverdims` とする。対象は Minecraft 1.21.1 の Nether / End であり、鉱脈生成、鉱石の品位・試掘、Ancient Debris および Netherite の処理は別 MOD に委ねる。Beneath は実装・資産の参照元であり、インストール必須 MOD ではない。

## Features

### F-001: Nether / End の TFC 気候

#### Description

TFC の `ClimateModel` を両 Dimension に適用する。Nether の基準気温は Beneath 1.21.x の設定値を採用する：Nether Wastes 70 ℃、Basalt Deltas 80 ℃、Crimson Forest 60 ℃、Warped Forest 60 ℃、Soul Sand Valley 55 ℃。高度補正は `-clampedMap(Y, 32, 128, -5, 5)` ℃とし、年間降水量は 0 mm とする。End は年間降水量 0 mm、平均・瞬間気温 20 ℃の固定気候とする。両者とも季節による気温・降水変動を設けない。TFC Calendar は既存のワールド時計を共用する。

#### Acceptance Criteria

##### AC-F001-001: Nether の気候値

Preconditions: Nether の5種類の Vanilla バイオームに検査地点があり、TFC Overdims と TFC が有効である。
Action: 各バイオームの Y=80 に対し TFC の平均気温、瞬間気温、平均降水量を取得し、さらに Nether Wastes の Y=32 と Y=128 を比較する。
Expected Result: Y=80 ではそれぞれ 70、80、60、60、55 ℃と降水量 0 mm を返す。Nether Wastes の Y=32 は 75 ℃、Y=128 は 65 ℃を返し、Calendar を進めても同一地点の気候値は変化しない。

##### AC-F001-002: End の気候値

Preconditions: End の主要バイオームに検査地点がある。
Action: 各地点の TFC 平均気温・瞬間気温・平均降水量を調べ、Calendar を複数日進める。
Expected Result: すべて 20 ℃、20 ℃、0 mm を返し、日付による変動が生じない。Overworld の気候計算は変更されない。

### F-002: Nether の Loose Rock と採掘

#### Description

`minecraft:netherrack` と `minecraft:blackstone` を置換せず、`tfcoverdims:loose_netherrack` と `tfcoverdims:loose_blackstone` を追加する。Loose Rock は TFC の地表小石と同じ 1～4 個表示・回収規則に従う。通常の非孤立採掘では Beneath のブロック戦利品設定を参照し、2～4 個の Loose Rock を落とす。TFC の `is_isolated` 条件を満たす場合は元のブロックを1個落とす。採掘対象の登録は岩石に限り、鉱石ブロックは変更しない。

#### Acceptance Criteria

##### AC-F002-001: Netherrack の戦利品

Preconditions: 隣接する Netherrack がある非孤立状態と、TFC の孤立条件を満たす Netherrack が用意されている。
Action: それぞれを適切な工具で破壊し、複数の乱数シードで結果を調べる。
Expected Result: 非孤立状態では Loose Netherrack が毎回2～4個、孤立状態では `minecraft:netherrack` が1個得られる。Nether Gold Ore、Nether Quartz Ore、Ancient Debris の戦利品は変更されない。

##### AC-F002-002: Blackstone の戦利品

Preconditions: 非孤立状態と孤立状態の Blackstone が用意されている。
Action: それぞれを破壊し、複数の乱数シードでドロップを調べる。
Expected Result: 非孤立状態では Loose Blackstone が毎回2～4個、孤立状態では `minecraft:blackstone` が1個得られる。Gilded Blackstone の戦利品は変更されない。

##### AC-F002-003: Loose Rock の配置と表示

Preconditions: Nether Wastes と Basalt Deltas を含む新規チャンクが生成される。
Action: 地表配置物とその回収動作、クライアント描画を確認する。
Expected Result: Netherrack 面には Loose Netherrack、Blackstone 面には Loose Blackstone が配置され、1～4個の見た目と回収個数が一致する。配置頻度は Beneath の対応する小石パッチを参照し、rarity 3、every-layer count を Netherrack=5、Blackstone=2 とする。鉱脈・地形の形状は変更されない。

### F-003: Netherrack と Nether Bricks の TFC 加工

#### Description

Beneath の Loose Netherrack → Vanilla Nether Brick の Chisel 加工、および4個の Brick と4個の TFC Mortar から4個の Nether Bricks を作る TFC 式加工を導入する。Cobblerack は追加せず、Loose Netherrack 4個から Vanilla Netherrack 1個を再構成し、Netherrack 1個をLoose Netherrack 4個へ戻せるようにする。Vanilla の Netherrack 製錬レシピと Nether Bricks のMortarを使わない製法は無効にする。既存の Nether Brick 系階段・ハーフブロック・塀などの Vanilla block / item ID は保持する。

#### Acceptance Criteria

##### AC-F003-001: Chisel と Mortar の加工

Preconditions: Loose Netherrack、TFC Chisel、`tfc:mortar` が用意されている。
Action: Loose Netherrack 1個とChiselで製作し、`N M N / M N M / N M N`（N=Nether Brick、M=Mortar）の3×3クラフトを行う。
Expected Result: ChiselはTFCの通常の製作時耐久減少を受け、`minecraft:nether_brick` 1個を取得する。次の製作で `minecraft:nether_bricks` 4個が得られ、各材料は規定数を消費する。

##### AC-F003-002: Netherrack の再構成と旧レシピ無効化

Preconditions: Loose Netherrack と Furnace 等が用意されている。
Action: Loose Netherrack 4個を2×2に並べる。生成したNetherrackを逆向きにクラフトし、別途 Vanilla の Netherrack 製錬およびMortarなしの Nether Bricks 製作を試す。
Expected Result: 2×2から Netherrack が1個、逆向きのクラフトからLoose Netherrackが4個得られる。旧製錬と旧Nether Bricksクラフトでは加工結果が得られない。他の Vanilla Nether Brick 派生ブロックは使用できる。

### F-004: Blackstone の TFC 加工

#### Description

Beneath の既存レシピを基準として Loose Blackstone 4個から `minecraft:blackstone` 1個を製作可能にし、Blackstone 1個を Loose Blackstone 4個へ解体可能にする。Loose Blackstone 1個とChiselから `tfcoverdims:blackstone_brick` 1個を製作する。Blackstone Brick 4個と TFC Mortar 4個のチェッカー配置から `minecraft:polished_blackstone_bricks` 4個を製作する。Polished Blackstone、Button、Pressure Plate、Cracked variant などは Vanilla の既存 ID を用いる。新しい独自石材familyは作らない。Loose BlackstoneをTFCのmetamorphic Stone Knapping素材として認識させる。

#### Acceptance Criteria

##### AC-F004-001: Loose Blackstone の相互変換

Preconditions: Loose Blackstone 4個と Blackstone 1個が用意されている。
Action: 2×2クラフトおよび逆向きのクラフトを別々に実行する。
Expected Result: 4個の Loose Blackstone から Blackstone 1個、Blackstone 1個から Loose Blackstone 4個が得られる。

##### AC-F004-002: Blackstone Brick の加工

Preconditions: Loose Blackstone、TFC Chisel、TFC Mortar がある。
Action: ChiselでBlackstone Brickを4個作り、Nether Bricksと同じ交互配置レシピを使用する。
Expected Result: `tfcoverdims:blackstone_brick` が1回につき1個作られ、4 Brick + 4 Mortarから `minecraft:polished_blackstone_bricks` 4個を取得する。Chiselは耐久を消費する。

##### AC-F004-003: Vanilla 派生素材との互換

Preconditions: Polished Blackstone Bricks、Blackstone Brick、TFC Hammer およびChiselがある。
Action: Beneathと同じ Hammer recipe で Cracked Polished Blackstone Bricks を作り、既存 Button / Pressure Plate のレシピを確認する。
Expected Result: Hammer 1回で Cracked Polished Blackstone Bricks が1個得られ、工具は耐久を消費する。Blackstone Brick 1個 + Chisel から Polished Blackstone Button 1個、2個 + Chisel から Pressure Plate 1個を作れる。既存の階段・塀・ハーフブロックは維持される。

##### AC-F004-004: Blackstone の石器加工

Preconditions: Loose BlackstoneとTFCのKnapping操作が利用できる。
Action: Loose Blackstoneから既存のTFC stone tool headをKnappingで製作する。
Expected Result: TFCのmetamorphic stone tool headが得られ、既存Stone Age工具へ加工できる。独自Stone Tool Tierや新規tool headは登録されない。

### F-005: End Stone の TFC 加工

#### Description

`minecraft:end_stone` と `minecraft:end_stone_bricks` を維持し、`tfcoverdims:loose_end_stone` および `tfcoverdims:end_stone_brick` を追加する。Loose Stone のブロックモデル、採掘量、加工レシピは TFC と Beneath の岩石モデルに合わせる。End専用の新規木材・鉱石・岩石装飾系列は追加しない。Loose End StoneはTFCのsedimentary Stone Knapping素材として扱う。

#### Acceptance Criteria

##### AC-F005-001: End Stone の採掘と配置

Preconditions: 非孤立と孤立の End Stone および新規Endチャンクがある。
Action: End Stoneを採掘し、地表生成したLoose End Stoneを確認する。
Expected Result: 非孤立でLoose End Stoneが2～4個、孤立時にはEnd Stoneが1個得られる。End Stone表面にTFC式1～4個のLoose Rockが自然配置され、その見た目・回収数が一致する。自然配置はrarity 3、every-layer count 5を基準とする。

##### AC-F005-002: End Stone のレンガ加工

Preconditions: Loose End Stone、TFC Chisel、TFC Mortarが用意されている。
Action: Loose End Stone 1個をChisel加工し、生成物4個とMortar4個を交互配置でクラフトする。別途Loose End Stone 4個を2×2で製作し、得られたEnd Stoneを逆変換する。
Expected Result: `tfcoverdims:end_stone_brick` 1個、`minecraft:end_stone_bricks` 4個、`minecraft:end_stone` 1個、およびEnd Stone 1個からLoose End Stone 4個をそれぞれ取得する。工具耐久とMortar消費が適用され、既存のEnd Stone Bricks派生材も機能する。

##### AC-F005-003: End Stone の石器加工

Preconditions: Loose End StoneとTFCのKnapping操作が利用できる。
Action: Loose End Stoneから既存のTFC stone tool headをKnappingで製作する。
Expected Result: TFCのsedimentary stone tool headが得られ、既存Stone Age工具へ加工できる。新規Tool Tier、独自石器モデルは追加されない。

### F-006: 崩落と採掘支保

#### Description

TFC既存のcollapse recipeとBlock Tagを用い、BlackstoneおよびEnd Stoneの採掘に崩落判定とSupport Beamによる防止を接続する。追加のCobbleブロックを作らず、崩落後も対応する既存ブロックとする。Netherrack、加工済みBrick、Vanilla Basaltおよび鉱石の崩落規則は変更しない。

#### Acceptance Criteria

##### AC-F006-001: Blackstone と End Stone の崩落

Preconditions: TFCで崩落判定が起きるよう配置されたBlackstoneおよびEnd Stoneの試験構造がある。
Action: TFCの採掘・崩落処理を開始する。
Expected Result: BlackstoneとEnd StoneはTFCの崩落対象になり、対応する既存ブロックとして落下する。新規Cobbleブロックを要求しない。

##### AC-F006-002: Support Beam と対象外ブロック

Preconditions: 同じ試験構造にTFC Support Beamを設置した状態と、Netherrack、Nether Bricks、End Stone Bricks、`minecraft:basalt`を設置した状態がある。
Action: 採掘と崩落判定を実行する。
Expected Result: Beamの保護範囲のBlackstone/End Stoneは崩落せず、列挙した対象外ブロックには本MOD由来の崩落処理が追加されない。TFCの`rock/raw/basalt`の動作は維持される。

### F-007: Vanilla Basalt の名称分離

#### Description

`minecraft:basalt`、`minecraft:polished_basalt`、`minecraft:smooth_basalt`の表示名のみを TFC Basalt と区別する。日本語名を「ネザー玄武岩」「磨かれたネザー玄武岩」「滑らかなネザー玄武岩」、英語名を「Nether Basalt」「Polished Nether Basalt」「Smooth Nether Basalt」とする。Registry、テクスチャ、モデル、Drop、Recipe、Worldgen、Rock Tagには変更を加えない。

#### Acceptance Criteria

##### AC-F007-001: 表示名とRegistryの維持

Preconditions: 日本語/英語クライアントに Vanilla の3種類のBasaltがある。
Action: アイテム名とブロック表示、Registry IDを確認する。
Expected Result: 指定された各言語の表示名になり、IDはすべて元の `minecraft:*` のままである。TFC Basaltの表示名は変更されない。

##### AC-F007-002: 地質システムとの分離

Preconditions: Vanilla Basaltと `tfc:rock/raw/basalt` を同一試験ワールドに置く。
Action: Loose Rock生成、採掘、崩落、レシピ、タグを確認する。
Expected Result: Vanilla Basaltに専用Loose Rock、TFC Basalt由来の変換レシピ・崩落・鉱石母岩登録が新規導入されない。TFC Basaltは従来どおり動作する。

### F-008: Crimson / Warped 木材の TFC 互換

#### Description

Vanilla の Crimson Stem/Hyphae と Warped Stem/Hyphaeを、Beneathの1.21.x木材対応を参考にTFC Lumberと木材加工・設備レシピへ接続する。必要なLumber/SupportおよびTFC既存設備の対応木材バリエーションを実装するが、独自の樹種、成長方法、Sapling、装置は追加しない。Vanillaの巨大菌類生成方法を維持する。

#### Acceptance Criteria

##### AC-F008-001: Lumber と Support

Preconditions: Vanilla Crimson/Warped Stem、TFC Saw、およびSupportの構成材料がある。
Action: TFCの製材・Support製作を実行する。
Expected Result: それぞれの樹種についてLumberと使用可能なSupportを取得でき、TFCの木材入力を使う対応レシピに利用できる。必要な処理量・工具耐久はBeneathの同等レシピに合わせる。

##### AC-F008-002: 木材設備と非可燃性

Preconditions: 両種の木材から作られたTFC木材設備とLumberがある。
Action: Chest、Barrel、Workbench、Tool Rackなど、TFC木材familyが実際に要求する設備を製作・設置・利用し、燃焼試験を実施する。
Expected Result: 対応設備はTFC標準仕様で利用できる。Crimson/WarpedのLumber、Planks、Support、設備はVanillaと同様に燃えず、Firepit燃料とCharcoal Pit原料として受け付けられない。

##### AC-F008-003: Vanillaの菌類生成維持

Preconditions: Crimson / Warped Fungusと対応Nyliumがある。
Action: Vanillaの巨大菌類生成操作を実行する。
Expected Result: 既存のCrimson/Warped Stem/Hyphaeで構成される巨大菌類が生成され、新規Sapling・独自樹木worldgenを必要としない。

### F-009: Nether Wart / Chorus の Calendar 対応

#### Description

Nether WartとChorus PlantはVanillaの成長土台、形状、成熟・採取結果を保ち、成長更新をTFC Calendarによる経過時間へ接続する。新規Farm Land、N/P/K以外の栄養素、独自作物は追加しない。時間進行の基準は各作物が既に持つVanillaの成熟過程とし、専用の成長ステージを増やさない。

#### Acceptance Criteria

##### AC-F009-001: Nether Wart

Preconditions: Soul Sand上にage 0のNether Wartがある。
Action: TFC Calendarの経過を反映させて成長を試験する。
Expected Result: Vanillaと同じage 0～3の範囲で段階が進み、最終的にNether Wartを採取できる。TFC専用農地・追加栄養素を要求しない。

##### AC-F009-002: Chorus

Preconditions: End Stone上にVanilla Chorus Flowerがあり、TFC Calendarを進められる。
Action: Calendarを進行させ、Chorusの成長・分岐・採取を確認する。
Expected Result: Vanilla Chorusの形状・分岐・Flowerの成長上限・Chorus Fruitの採取結果を保持し、Calendarの経過に応じて成長更新される。

### F-010: 食料・水分の TFC 互換

#### Description

Hoglin由来の食肉をTFC Porkへ、Vanilla Chorus FruitをTFCの既存Food・Decay・Nutrition体系へ接続する。Chorus Fruitのテレポート効果は維持する。Netherの高温時もTFCの渇き計算を有効にし、設置水のVanilla蒸発は維持する一方、TFC容器内の水は通常どおり保持する。新規食品・Juicerなどは追加しない。

#### Acceptance Criteria

##### AC-F010-001: Hoglin 食肉

Preconditions: NetherにHoglinが存在する。
Action: Hoglinを倒して肉のドロップと調理・腐敗を確認する。
Expected Result: Lootingなしの場合、既存TFC Porkを2～4個、Boneを3～6個取得し、TFC食品の腐敗と調理規則が適用される。Beneath固有のCursed Hideは出現しない。

##### AC-F010-002: Chorus Fruit の食品情報

Preconditions: Chorus Fruitを持ったプレイヤーがEndにいる。
Action: 食品情報とCalendar経過後の腐敗状態を確認し、食べる。
Expected Result: TFC Food/Decay/Nutritionが適用され、適切に保存・腐敗が処理される。腐敗していないもののTeleport動作はVanillaのままである。

##### AC-F010-003: Nether の水と渇き

Preconditions: NetherにTFC Jug/Barrelなどの水入り容器、水バケツ、プレイヤーがいる。
Action: 容器を使用し、Water Sourceを設置し、TFCの渇き値を測定する。
Expected Result: 容器に格納した水は消失せず飲用・貯蔵でき、通常の設置水はVanillaどおり蒸発し、TFCの渇きは気温に応じて減少する。

### F-011: Vanilla Mob / Piglin の互換

#### Description

Nether・EndのVanilla MobとAIは維持し、TFCの既存Loot、装備・素材に接続できる部分だけ調整する。Mobを追加・置換しない。TFCに存在しないGold Armorを新設しないため、Piglinの金装備判定はVanilla Gold Armorを保持する。Barteringの入力にTFC Gold Ingotを受け付ける。Netherite関連Drop、鉱石素材や鉱脈の仕様には介入しない。

#### Acceptance Criteria

##### AC-F011-001: Piglin の金インゴット互換

Preconditions: Piglin、TFC Gold Ingot、Vanilla Gold IngotおよびVanilla Gold Armorがある。
Action: 両インゴットでBarteringを行い、Gold Armorを装備してPiglinの反応を確認する。
Expected Result: TFC Gold IngotをVanilla Gold Ingotと同様に交換品として受け取り、装備による敵対抑制はVanillaのまま動作する。新規Gold Armor itemは追加されない。

##### AC-F011-002: 既存Mobの互換ドロップ

Preconditions: NetherのHoglin、Piglin、Zombified PiglinとEndのEnderman、Shulkerがいる。
Action: 各Mobを倒し、ドロップの種別、TFCサイズ規則、Netherite関連ドロップへの影響を確認する。
Expected Result: TFCに既存の対応品があるドロップは既存TFC品を使用し、固有のBlaze Rod、Ghast Tear、Ender Pearl、Shulker Shellなどは維持する。新規Mob・独自生物素材・Netherite変更は発生しない。

### F-012: Nether / End 構造物チェストの互換

#### Description

Nether Fortress、Bastion Remnant各チェスト、Ruined Portal、End CityのLoot Tableから、TFCを迂回するVanilla完成金属装備・Ingot・食品をTFCの対応品へ置換する。Beneathの1.21.xチェスト定義からVanilla構造物固有品を保持したTFC候補と重みを参照する。例えばNether FortressのTFC Diamond Gem、Wrought Iron Ingot、Gold Ingot、Wrought Iron Sword、Wrought Iron Chestplateの重みはそれぞれ5、Saddleは10、Bismuth Bronze Horse Armorは8とする。Bastion TreasureのBlack Steel防具の重みは各6、Black Steel Swordは7、Diamond/Opal/Ruby Gemは各10とする。Nether Fortressの主poolは2～4回、Bastion Treasureの主poolは3回・副poolは3～4回を基準とする。Beneath固有品、独自エンチャント品は追加しない。Ancient Debris、Netherite、Smithing Templateのlootには触れない。

#### Acceptance Criteria

##### AC-F012-001: Nether Fortress と Bastion

Preconditions: Nether FortressおよびBastion各種のLoot Tableを参照できる。
Action: 異なる乱数シードでチェストを生成し、出現品・重み・roll数を確認する。
Expected Result: Fortressの主poolは2～4回、Bastion Treasureの主poolは3回と副pool3～4回で、Beneathが用いる既存TFC装備・Gold/Pig Iron・Gem等へ必要箇所だけ置換される。Beneath専用品は出ず、Netherite関連の既存lootは保持される。

##### AC-F012-002: Ruined Portal と End City

Preconditions: Ruined PortalとEnd Cityの既存Loot Tableがある。
Action: 複数の乱数シードでチェストの中身を検査する。
Expected Result: 金属系Vanilla完成装備はTFCの該当進行段階の品へ置換され、Obsidian、Ender Pearl、Elytra、Shulker ShellなどのDimension固有報酬と、Netherite関連報酬は保持される。独自進行アイテムは出現しない。

### F-013: Vanilla Enchantment と TFC の互換

#### Description

Powered TFC の `EnchantmentMenuLapisMixin` を参考に、Vanilla の Enchantment Table のラピスラズリ入力枠で `minecraft:lapis_lazuli` と `tfc:gem/lapis_lazuli` の両方を受け付ける。`#tfcoverdims:enchanting_lapis` Item Tag に両方を登録し、tag読み込みが失敗した場合にも両Registry IDを受け付ける。通常投入とShiftクリックの両方に対応し、既存スロットの位置・アイコン・消費量は変更しない。TFC完成装備については標準Enchantmentとの互換を検証して不足するItem Tag / Componentだけを補完し、Forging Bonusを保持する。新規エンチャント、Tome、専用GUIは追加しない。

#### Acceptance Criteria

##### AC-F013-001: TFC Lapis Lazuli Gem のエンチャント台投入

Preconditions: Enchantment Table、`minecraft:lapis_lazuli`、`tfc:gem/lapis_lazuli` およびEnchant可能な既存装備がある。
Action: 両素材を順にラピスラズリ枠へ通常投入し、必要な経験値を用意してEnchantを実行する。
Expected Result: 両素材とも同じ入力枠で受け付けられ、Vanillaと同数の素材を消費してEnchantに成功する。枠の位置・既存アイコン・Enchantment選択規則は変わらない。

##### AC-F013-002: ShiftクリックとTagの互換

Preconditions: Enchantment Tableを開き、両ラピス素材をインベントリに持つ。`#tfcoverdims:enchanting_lapis` には両IDが登録されている。
Action: それぞれShiftクリックでメニューへ移す処理を実行し、Tag参照が利用できない場合の入力判定を検査する。
Expected Result: 両素材はラピスラズリ枠へ移動し、別スロットには誤移動しない。Tagを利用できない場合も両IDを直接照合して投入できる。

##### AC-F013-003: 完成TFC装備とForging Bonus

Preconditions: 完成Steel Sword、Steel Pickaxe、Steel Chestplate、Forging Bonus付き完成工具、未完成Tool Head、Metal Sheet、および対応するVanilla Enchanted Bookがある。
Action: 標準Enchantment/Anvil経由で適合するEnchantを付与し、Forging Bonus付き工具の耐久消費と中間部品の適格性を確認する。
Expected Result: 完成装備だけが適合するVanilla Enchantを保持・発動し、既存Forging Bonusと両立する。未完成部品には新たなEnchant適性を与えず、新規Enchant・新規GUIは登録されない。

### F-014: Shulker Box と TFC 収納規則

#### Description

既存Shulker Boxを残し、TFCのItem Size制限および容器の入れ子制限を迂回する経路があれば修正する。染色、破壊時の内容保持、設置・再配置、保存・再読込を維持する。新規収納設備は作らない。

#### Acceptance Criteria

##### AC-F014-001: アイテムサイズと入れ子

Preconditions: TFCの収納可否が異なる複数サイズのアイテムとShulker Boxがある。
Action: Shulker Boxへの収納・入れ子操作を試す。
Expected Result: TFCが禁止する収納と容器入れ子はShulker Box経由でも成立せず、許可された収納は正常に行える。

##### AC-F014-002: Vanilla Shulker Boxの保存

Preconditions: 許可されたTFCアイテムを収納したShulker BoxがEndに置かれている。
Action: 破壊、持ち運び、再設置、ワールド保存・再読み込みを行う。
Expected Result: 内容とItem Componentが保持され、色・モデル・動作はVanillaのままである。

### F-015: Portal と既存TFC設備のDimension動作

#### Description

Portalの生成条件、Fortress・Strongholdの攻略方法、Ender Dragon、ElytraはVanillaから変更しない。既存TFC設備をNether/Endへ持ち込んだ場合、Dimensionを理由として動作が失敗する箇所のみ互換修正する。新規Portal素材・専用Forge・専用Anvil・儀式を追加しない。

#### Acceptance Criteria

##### AC-F015-001: 既存Portalの維持

Preconditions: Vanilla Obsidian Portalと、Eye of Enderを使用できるStronghold End Portalがある。
Action: 両PortalをVanillaの方法で開き、Dimension間を移動する。
Expected Result: NetherとEndへ移動でき、Portalの形状・必要素材・生成条件はVanillaから変わらない。独自Portal itemは不要である。

##### AC-F015-002: TFC設備の持ち込み

Preconditions: Nether/Endそれぞれに既存のTFC Firepit、Barrel、Anvilがある。
Action: Firepitの加熱、Barrelの貯蔵、Anvilの既存加工を実行する。
Expected Result: TFCと同じレシピ・耐久・温度・容器規則で処理される。独自のDimension専用設備は生成されない。

### F-016: 既存アセットと表示の統合

#### Description

新規アイテム・地表小石・TFC木材設備のモデルとテクスチャは、TFC 4.2.12またはBeneath 1.21.xの既存資産を再使用する。色調変更が必要な場合も、それらの既存資産を変換した派生物だけを同梱し、新規の手描き・生成画像を作らない。Vanillaの既存blockはVanillaのテクスチャ参照を維持する。クライアント表示は日本語・英語に対応する。

#### Acceptance Criteria

##### AC-F016-001: 追加資産の表示と参照整合性

Preconditions: 本MODのLoose Rock 3種、Brick Item 2種、Crimson/Warped Lumber・Support等がクライアントにある。
Action: インベントリ、地表配置、設置済みブロックを表示し、同梱画像と使用元を監査する。
Expected Result: Missing Texture、モデル欠落、UV不整合がなく、追加テクスチャはTFC/Beneathの原本または色調変換版である。親モデル・テクスチャの参照が解決でき、使用する原本と変換規則を実装仕様から確認できる。

##### AC-F016-002: 表示言語と互換メニュー

Preconditions: クライアントの言語を英語または日本語へ切り替えられる。
Action: 新規アイテム名、レシピ表示、Vanilla Basalt表示名、TFCツールチップを確認する。
Expected Result: 両言語で名称が表示され、追加アイテムの未翻訳キーやVanilla/TFC Basaltの同名混同がない。独自GUI・専用Enchant画面は存在しない。

##### AC-F016-003: 新規アイテムの TFC Size / Weight

Preconditions: 本MODが登録するLoose Rock、Brick中間アイテム、Lumber、Support、木材設備のItemStackがある。
Action: TFCのItem SizeとItem Weightを取得し、対応するTFC既存アイテムと比較する。
Expected Result: すべての新規アイテムに有効なSize/Weightが1通りだけ設定され、同等のTFC Loose Rock、Brick、Lumber、Support、設備と同じ分類になる。未分類の追加アイテムはない。

## Global Requirements

- Git管理ファイルと配布JARには環境固有の絶対パス、利用者名、端末名、メールアドレス、認証情報、アセット別のライセンス・帰属・クレジット記載を追加しない。ローカル設定・検証証跡はGit管理外に置き、LICENSE / LICENCEを含む名前の既存ファイルは変更しない。
- 実行時に直接必須とするMODは **TerraFirmaCraft 1.21.1-4.2.12 以上の、Minecraft 1.21.1に対応する4.2.x版**のみとする。TFCが自身のメタデータで要求する必須依存はTFC側に従い、本MODが別途必要としない依存を追加しない。Beneathは参照元であり、実行時依存にしない。
- Minecraft/NeoForgeの環境は上記Platformの固定バージョンでビルド・試験する。NeoForge 21.1.262は現行の1.21.1対応リリースとして採用し、Minecraft 1.21.1とJava 21の範囲を変更しない。
- TFCが既に提供するAPI、Data Component、Recipe、Tag、Loot Condition、Climate Model、Calendarを優先し、同等機能を二重実装しない。TFC 4.2.12で要件を既に満たす機能には不要な上書きを行わない。
- Recipe・Block Tag・Loot Table・Biome Modifierで表現できる設定はJSON/Data Packを基本とし、Javaでは既存APIとの接続と不足処理だけを実装する。Worldgen変更はF-002/F-005の地表小石に限る。
- 全Featureの各ACにはGameTestまたはE2Eを対応させる。ロジックはGameTest、表示はE2Eで検証し、両方を要求するACには両方の証跡を残す。Dedicated Serverとクライアントの両方で起動する。
- 変更したLoot Tableでは複数の固定乱数シードで生成結果をテストする。抽選確率の数値は参照したBeneath/TFCのデータを優先し、非対象のVanilla報酬は維持する。
- 新規登録したすべてのアイテムにはTFC既存の類似品を基準とするItem Size/Weightを割り当て、未分類と重複を許さない。新規テクスチャは制作しない。TFC/Beneathの既存ファイルをコピーまたは色調変換して使用し、変換内容・元ファイルパス・取得リビジョンをソース管理で確認できるようにする。Vanillaリソースは新規同梱せず、既存テクスチャを参照する。
- 参照実装：TFC `https://github.com/TerraFirmaCraft/TerraFirmaCraft` の `v4.2.12`、Beneath `https://github.com/eerussianguy/Beneath` の `1.21.x`、Harness `https://github.com/lau-mods/modding-harness/tree/rebuild2`。Beneathについては岩石・木材・気候・Lootの該当部分だけを参照する。

## Constraints

- ベースJava packageは `net.claustra01.tfcoverdims`、Mod IDは `tfcoverdims` とする。既存の `minecraft:*`、`tfc:*` のRegistry IDは置換しない。
- 新規登録を許可する岩石関連の最小資源は `loose_netherrack`、`loose_blackstone`、`loose_end_stone`、`blackstone_brick`、`end_stone_brick` および必要な配置・木材互換アイテムに限る。`minecraft:nether_brick`は再利用する。Cobblerack、独自鉱石、Hellbricks等は追加しない。
- `minecraft:basalt`・`minecraft:polished_basalt`・`minecraft:smooth_basalt`は表示名のみ変更する。`tfc:rock/*/basalt`と同一視しない。Loose Basalt、Basalt専用Brick、地質Tag、生成処理、採掘規則を追加しない。
- 鉱脈生成、鉱石品位、Prospector's Pickの追加鉱石対応、Nether Gold/Quartz Ore、Ancient Debris、Netherite素材・装備・製錬・Upgradeは本MODの対象外とする。これらを担当する別MODの有無を必須条件としない。
- 新規Dimension、独自Portal、Ancient Altar、Lost Page、Tome、Hellforge、Burpflower、Unposter、Juicer、Red Elk、独自栄養素・植物・作物・魔術・エンチャントは実装しない。
- TFCの既存工具・完成装備の性能を再設計せず、Enchantmentの適用互換だけを扱う。未完成部品にEnchant適性を追加しない。
- Vanilla Nether/Endの地形、既存構造物、Boss、Elytra、Shulker、Chorus、Piglin AI、既存Dimension固有報酬を保持する。
- 上記Featuresの達成に不要な任意MOD統合、互換アドオン、独自コンフィグ画面、追加アニメーション・独自グラフィック・新規Beneath依存を実装しない。

## Open Questions

None.
