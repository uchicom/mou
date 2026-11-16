# mou 設計書 改訂版（Rev.6）

状態: レビュー待ち。Rev.6 は Rev.5 の軽微な不整合の修正（§9.2）。Rev.5 は発送元在庫の二重使用の防止に限定した補正。**この改訂は設計書のみの変更であり、Java コードの実装・変更・削除・移動、Maven 設定・CI・GitHub Issue の実行や変更は一切行っていない**（削除・移動は §8 の一覧を承認後、Issue #1 で行う）。

- §1 決定事項 / §2 未確定事項 / §3 ADR / §4 設計 / §5 物資保存則と期待値 / §6 テスト計画 / §7 Issue / §8 anko 資産の再利用方針 / §9 Rev.4 変更一覧
- Rev.5 からの変更（詳細は §9.2）: 未輸送量の値の参照タイミングの明確化と旧注記の修正（§4.6）、発送元在庫の逐次配分テストの shortage 期待値の追記（§6）。
- Rev.4 からの変更（詳細は §9.2）: 発送元在庫の二重使用の防止（ADR-002, §4.5 手順 3・7・8, §4.6, §5.1, §6）。
- Rev.3 からの変更（軽微な補正のみ。詳細は §9.1）: 未輸送量の定義の補足（ADR-005, §4.6）、SimClock と World の責務分担（§4.1, §4.2, §4.5）、道路グラフの閉路処理（ADR-006, §4.4）、Maven 依存関係の検証（§6）、Demand Slot の追加テスト（§6）。
- Rev.2 からの変更: Q0 の修正（§1.1・§8）、都市の需要枠（ADR-009）、FILL 需要量、道路セルの幅規則（§4.4）、Dijkstra のタイブレーク（ADR-004）、Resource の tick 規則（ADR-010）、乱数の削除、入力検証とオーバーフロー対策（§4.7）、U1〜U7 の確定。

---

## 1. 決定事項

### 1.1 Q0〜Q7（確定）

| # | 決定 |
|---|---|
| Q0 | anko 由来のコードを**一律に削除しない**。汎用ユーティリティ・ログ・設定管理・ビルド設定を調査し、再利用候補と不要なものに分ける（結果は §8）。**mou のコア（`com.uchicom.mou` 配下）は anko 固有のクラス・ライブラリに依存しない**。削除・移動は §8 の一覧の承認後に Issue #1 で実施する |
| Q1 | 施設（農場・採取施設・倉庫・都市）はシナリオまたは設定ファイルで座標指定する。ビットマップは地形・土地利用・道路などの初期条件に使う |
| Q2 | 道路は双方向を基本とし、方向ごとに独立した容量を持つ。一方通行へ拡張できる構造とする（§4.3, §4.4） |
| Q3 | 在庫上限は施設の設定値。満杯時の生産抑制量を記録する |
| Q4 | 都市の不足分は次 tick へ繰り越さず、shortage として記録する |
| Q5 | 同一 tick に入庫した物資は再出荷しない。倉庫経由で最低 1 tick の遅延を許容する |
| Q6 | 道路幅は設定値を使い、ビットマップから推定しない |
| Q7 | 容量配分は priority 昇順 → ID 昇順の逐次 greedy |

### 1.2 設計修正（確定）

| # | 修正 | 反映先 |
|---|---|---|
| R2-1 | `long` 演算のオーバーフローを検出し、発生したらシミュレーションを失敗させる | ADR-003, §4.7 |
| R2-2 | 倉庫の貨物別在庫と施設全体の在庫容量を分けて定義する | §4.3 |
| R2-3 | Resource の `maxAmount` / `current` の意味を明確化する | §4.3, ADR-010 |
| R2-4 | ビットマップ道路 → Node/Edge の生成規則（4 方向接続） | ADR-006, §4.4 |
| R2-5 | 経路コストは所要時間のみ。輸送費は別集計 | ADR-004 |
| R2-6 | 生産抑制量・未輸送量・shortage・累計生産量・累計消費量を区別する | ADR-005, §4.6 |
| R3-2 | 同一都市・同一貨物を対象とする複数 request は都市・貨物別の需要枠を priority 順に配分し、二重計上しない | ADR-009, §4.5 |
| R3-3 | FILL の需要量 = 貨物別空き容量と施設全体空き容量の小さい方 | §4.5 手順 4 |
| R3-4 | 道路セルは原則 1 セル幅の中心線。2×2 以上の塊は設定エラー | ADR-006, §4.4 |
| R3-5 | Dijkstra の同一所要時間はコスト → Edge ID で決定的にタイブレーク | ADR-004 |
| R3-6 | Resource の tick 起点と、採取 → 再生の順序 | ADR-010 |
| R3-7 | MVP では乱数を持たない（`RandomGenerator` を `SimulationConfig` に置かない）。必要になった段階で ADR を追加して導入 | §1.3 |
| R3-8 | 所要時間計算の入力値検証とオーバーフロー対策 | ADR-003, ADR-004, §4.7 |

### 1.3 引き続き有効な方針

- 1 tick = 1 時間。物資量は `long`（kg）、能力は t/h を設定値とし kg/tick へ変換（×1000）。
- 距離は m（セル数 × `cellSizeMeters`）で保持する。
- **MVP は乱数を使わない。** 同一の初期条件・設定であれば結果は完全に一致する。
- 反復順序は ID 昇順の配列/`List`。`HashMap` の反復順に依存しない。
- コアは描画を知らず、`TickResult`（不変）だけを返す。Seed（不変）と World（可変）は型で分離する。
- 容量・需要・供給・在庫を表す小さな値型のみ導入し、インターフェース階層は作らない。

### 1.4 未確定事項だった U1〜U7（確定）

| # | 決定 |
|---|---|
| U1 | 単一 Maven モジュール。コアの依存は JDK のみ（テストのみ JUnit + AssertJ）。依存方向は §8 のテストで機械的に検証 |
| U2 | Java コードのシナリオ + `.properties`（JDK の `Properties` で読込） |
| U3 | 農場の出力在庫上限 = 100,000 t（基本シナリオ） |
| U4 | 基本シナリオは道路容量を直接指定（70 / 50 t/h） |
| U5 | 土地利用は SeedMap に保持するだけで、計算に使わない |
| U6 | 都市以外の消費先・必要投入物は MVP 対象外 |
| U7 | Farm は Resource を使わない。Harvester のみ Resource を使う |

---

## 2. 未確定事項

実装中に決める小さな項目のみ。該当 Issue の着手前に確認する。

| # | 項目 | 決める時期 |
|---|---|---|
| O1 | ビットマップ由来の道路の既定値（幅・速度・容量係数・輸送費） | Issue #4 着手前 |
| O2 | `.properties` のキー設計（施設の座標指定、ColorPalette の既定色） | Issue #3 着手前に案を提示 |
| O3 | 2×2 以上の道路塊を将来「細線化して許容」するか | MVP 後。MVP はエラー |
| O4 | §8 の削除・移動の対象一覧 | 承認後に Issue #1 |

---

## 3. ADR

### ADR-001 輸送は 1 tick 内の瞬時移動。道路は貯蔵しない（決定）
道路は 1 tick に通せる量（スループット）。配送は同一 tick 内で発送元在庫 → 配送先在庫へ確定移動する。移動量は経路上の全制約の最小値で決まる。受入側で止まる分は**そもそも出荷しない**ので発送元に残り、道路上に宙に浮く物資は存在しない。1 tick を超える輸送遅延は MVP 対象外。経路は容量を考慮しない固定の最短時間経路（残容量による迂回は MVP 対象外）。

### ADR-002 容量配分は priority 昇順 → ID 昇順の逐次 greedy（決定: Q7）
request を順に処理し、確定した量をその都度、経路上の各エッジ残容量・受入側の残り受入能力/空き/需要枠から減算する。決定的で単純。最大流・比例配分は将来拡張。
**発送元在庫も同様に逐次消費する**: 発送元施設・貨物ごとに「残り出荷可能量」を持ち（初期値 = 手順 3 のスナップショット在庫）、request を処理するたびに確定した `shipped` を減算する。複数の request が同じ発送元施設・同じ貨物を参照しても、先行 request が割り当てた在庫を後続 request が重ねて使うことはない（在庫の二重使用の禁止）。スナップショット在庫そのものは tick 中変更せず、各 request はその時点の残り出荷可能量だけを参照する。

### ADR-003 オーバーフローは検出して失敗させる（決定）
- 量・時間・距離の加減乗算は `Math.addExact / subtractExact / multiplyExact / ceilDiv` を使う専用ヘルパー（`Quantity`）に限定し、素の `+ - *` を禁止する。
- 実行時に発生したら `SimulationFailedException(tick, phase, 原因)` を送出し、`Simulation` は `FAILED` となり以降の `step()` を拒否する。部分更新された World は再利用しない。
- セットアップ時（設定読込・経路計算）に発生したら `InvalidConfigurationException`。
- 「受入能力無制限」は `Long.MAX_VALUE` を番兵とし、`min` にのみ使い、加算には使わない。「到達不能」の経路コストも同じ番兵で、加算の前に必ず判定する。
- 入力値の範囲検証は §4.7。

### ADR-004 経路コストは所要時間のみ。決定的なタイブレーク（決定）
- **コスト** = 経路上の各 Edge の所要時間（秒, `long`）の合計。
  `travelSeconds = ceilDiv(lengthM × 36, speedKmh × 10)`（= `ceil(lengthM × 3600 / (speedKmh × 1000))`）。整数のみ。入力検証は §4.7。
- **タイブレーク**（同一合計所要時間の経路が複数ある場合）。Dijkstra を次のように定義し、結果がグラフと始点・終点だけで一意に決まるようにする。
  1. 優先度キュー順序: `(累計コスト, Node ID)` の昇順。
  2. 緩和: 各 Node は `(最小累計コスト, 到達 Edge ID)` を保持する。候補の累計コストが小さければ更新。**累計コストが等しい場合は到達 Edge ID が小さい方を採る**。
  3. 並行 Edge（同一 Node 対）は Edge ID が小さい方が優先される（上記 2 により）。
  4. 経路復元は終点から到達 Edge を逆に辿る。
  これは「決定的」であることを保証する規則で、全経路の辞書順最小を保証するものではない。
- **輸送費**は経路選択に使わない。`Edge.costPerTon`（整数通貨単位）× 輸送量(kg) を `kg·単位` の `long` で累積し、報告時に ÷1000 する（丸め誤差なし）。

### ADR-005 数量指標の区別（決定）
§4.6。生産抑制量・未輸送量・shortage・累計生産量・累計消費量は別の量であり、互いに代用しない。
未輸送量は「発送元に物資があり、需要も存在するが、輸送上の制約で運べなかった量」であり、制約には経路未接続・道路容量不足・受入能力不足・受入側の空き容量不足・その他輸送可能量を制限する条件を含む。発送元に物資が存在しないために運べなかった量は含めず、都市の shortage にのみ反映する。

### ADR-006 ビットマップ道路 → Node/Edge 規則（決定）
§4.4。上下左右の 4 方向接続のみ。道路セルは 1 セル幅の中心線とし、2×2 以上の塊は設定エラー。閉路を含む道路構造は、道路セル間の無向接続を一度だけ処理し、Node から Node までの道路区間を Edge 化する規則で扱う（§4.4「閉路を含む道路構造」）。

### ADR-007 同一 tick 再出荷禁止とスナップショット（決定: Q5）
出荷可能量・受入側の空きは**手順 3（生産反映）完了時点のスナップショット**で決める。同 tick に入庫した物資は再出荷されず、同 tick の出庫分は空きとして数えない（保守的）。1 つの倉庫を経由するたびに 1 tick 遅延する。

### ADR-008 都市の不足分は繰り越さない（決定: Q4）
消費は後から取り戻せないため次 tick の需要に上乗せしない（二重計上防止）。shortage として tick 別・累計で記録する。

### ADR-009 需要枠（Demand Slot）による二重計上の防止（決定）
- 都市の消費需要は**貨物別**に `consumptionDemand[cargo]`（kg/tick）で定義する。都市全体の `acceptCapacity`（kg/tick）は貨物をまたいで共有する。
- tick ごとに手順 4 で、都市 c・貨物 g ごとに**需要枠** `slot[c][g] = consumptionDemand[g]` を作る。
- 都市 c・貨物 g を宛先とする request を priority 昇順 → ID 昇順に処理する。各 request の `desired = slot[c][g]`（その時点の残り枠）。確定量 `shipped` を `slot[c][g]` と都市の残り受入能力から減算する。
- 結果、同じ都市・貨物に複数 request があっても、合計の入荷は `min(consumptionDemand[g], 受入能力の残り)` を超えない。
- `shortage[c][g] = consumptionDemand[g] − Σ shipped`。未輸送量は request ごとに §4.6 の式で、`desired` はその request 処理時の残り枠。
- 倉庫（FILL）の宛先についても、貨物別・施設全体の**残り空き**を request ごとに減算していく同じ方式（空きの二重使用なし）。

### ADR-010 Resource の tick 規則（決定）
- `SimClock.tick` は開始時 0。手順 1 の `advance()` の後の値を、その tick の番号とする（最初に処理される tick は **1**）。
- 再生は `regenIntervalTicks ≥ 1` で、`tick % regenIntervalTicks == 0` の tick に行う（最初の再生は tick = `regenIntervalTicks`。tick 0 では再生しない）。
- 1 tick 内の順序は **採取 → 再生**。採取可能量は tick 開始時点の `current`（手順 2・3）。再生はその後で `min(regenPerTick, maxAmount − 採取後の current)` を加算する。同 tick の再生分は同 tick には採取できない（ADR-007 と同じ保守的規則）。

### ADR-011 乱数は MVP では導入しない（決定）
`SimulationConfig` は乱数を保持しない。導入が必要になった段階で、seed の指定方法・乱数を使うフェーズ・再現性テストを含む ADR を追加する。

### ADR-012 anko 資産の扱い（決定: Q0）
§8。再利用候補は mou の依存方向（mou → 汎用機能のみ）を守る形で取り込み、コアから anko 固有のものを参照しない。

---

## 4. 設計

### 4.1 パッケージ構成（`com.uchicom.mou`）

```
seed        SeedMap（不変）, BitmapSeedLoader, ColorPalette(設定可能), CellType
world       World（可変状態。時刻を持たない）, GridMap
network     RoadNetwork, Node, Edge, RouteFinder(Dijkstra), RouteCache, RoadGraphBuilder
resource    Resource, CargoType
facility    Facility 系: Farm, Harvester, Warehouse, City, Stock
logistics   TransportRequest, DemandSlot, FlowPlanner, FlowResult
sim         Simulation（SimClock を保持）, SimClock, SimulationConfig, TickWorkspace, Quantity,
            SimulationFailedException, InvalidConfigurationException
report      TickResult, CsvResultWriter, ConsoleReport
scenario    LinearChainScenario
support     （再利用する汎用機能。§8。コアのパッケージから参照しない）
```

ID は `int` 連番、状態は `long[]` 等の配列。外部ライブラリは使わない（テストのみ JUnit + AssertJ）。

### 4.2 時間
`SimClock`: 1 tick = 1 h、1 日 = 24 tick、1 年 = 8,760 tick。番号規則は ADR-010（開始時 0、最初に処理される tick は 1）。

**責務分担（SimClock と World）**
- `Simulation` が `SimClock` を保持する。`SimClock` はシミュレーションの進行時刻を管理する。
- `World` はシミュレーション対象の可変状態（在庫・Resource・Edge 利用量・累計値など）を保持し、**時刻進行の責務を持たない**。
- 時刻を必要とする処理（Resource の再生判定など）は、`Simulation` が advance 後の tick 番号を値として引数で渡す。`World` は `SimClock` への参照を持たない。
- したがって手順 1 の `SimClock.advance()` は World への書き込みではない（§4.5）。

### 4.3 データモデル

**CargoType**: `AGRICULTURE`, `TIMBER`（enum）。

**Stock**（施設の在庫。貨物別在庫と容量を分離）
- `amount[cargo]`: 貨物別の現在在庫（kg, ≥ 0）。
- `totalCapacity`: **施設全体**の在庫容量（kg）。常に `Σ amount[c] ≤ totalCapacity`。
- `cargoCapacity[cargo]`（任意）: 貨物別の上限。省略時は `totalCapacity`。
- **貨物 c の空き = `min(cargoCapacity[c] − amount[c], totalCapacity − Σ amount)`**。入庫はこの空きを超えられない。
- 倉庫は Stock を 1 つ持つ。農場・採取施設は「出力在庫」として同じ Stock を持つ（`totalCapacity` = 出力在庫上限, Q3。基本シナリオは 100,000 t）。

**Resource**（共通の再生可能資源モデル）
- `maxAmount`: 資源の**上限量**（仕様の「総資源量」に相当）。再生で `current` がこれを超えることはない。
- `current`: **現在の採取可能量**（0 ≤ current ≤ maxAmount）。
- `maxExtractPerTick`: 1 tick の最大採取量。
- `regenPerTick`, `regenIntervalTicks`（≥ 1）: 再生の量と周期。再生しない資源は `regenPerTick = 0`。
- 採取量 = `min(要求量, maxExtractPerTick, tick 開始時点の current)`。再生は ADR-010。
- 累計採取量 `cumExtracted` と累計実再生量 `cumRegenerated` を保持（保存則の検証用）。

**Facility 共通**: `id, kind, nodeId`（接続する道路 Node）。
- **Farm**: `productionCapacity`（kg/tick）、`outputStock`、`enabled`。Resource を使わない（U7）。
- **Harvester**: Farm と同じ項目 + `Resource` への参照。実生産量は Resource の採取可能量でも制限される。
- **Warehouse**: `stock`。生産も消費もしない。
- **City**: `consumptionDemand[cargo]`（kg/tick）、`acceptCapacity`（kg/tick, 貨物共有）。在庫を持たず、受け入れた分はその tick に消費として確定。

**RoadNetwork**: 有向 Edge のリスト。
- `Node{id, cellX, cellY}`。
- `Edge{id, fromNode, toNode, lengthM, widthM, capacityKgPerTick, speedKmh, costPerTon, reverseEdgeId(-1 可)}`。
- 容量 = `widthM × capacityCoefficient`（kg/h per m, 設定値）、またはシナリオでの直接指定。
- 容量は Edge（方向）ごとに独立（Q2）。双方向は 2 本の有向 Edge。一方通行は逆 Edge を作らず `reverseEdgeId = -1`。

**TransportRequest**: `id, fromFacility, toFacility, cargo, priority, kind`。`kind` は `FILL`（倉庫の補充）または `CONSUME`（都市の消費）。誰が誰に運ぶかは明示的に定義する。

### 4.4 ビットマップ道路 → Node/Edge 生成規則

**前提（道路セルの幅）**: 道路セルは原則 **1 セル幅の中心線**を表す。道路幅は設定値であり、ビットマップ上の太さでは表現しない（Q6）。

**入力検証（2×2 以上の道路塊）**
- 2×2 の窓の 4 セルがすべて道路セルである箇所を「塊」と呼ぶ。1 つでも検出したら**設定エラー**（`InvalidConfigurationException`）とし、塊を構成するセルの座標を（行優先で最大 N 件）メッセージに含める。自動で細線化・補正はしない（O3）。
- 斜めにしか接していない道路セル（4 近傍で接続しない）は別の道路として扱う。接続しないので、検出した場合は警告として座標を報告する（エラーにはしない）。

**生成規則（4 方向接続）**
1. **接続**: 2 つの道路セルは上下左右の 4 近傍で隣り合う場合のみ接続する。
2. **次数**: 各道路セルの 4 近傍の道路セル数。
3. **Node にするセル**: ①次数 ≠ 2（端点・分岐・交差・孤立）、②施設の接続セル、③マップ外周上の道路セル（外部接続の予約）。
4. **次数 2 のセル**（直線・曲がり角どちらも）は Node にせず、2 つの Node の間の Edge に畳み込む。各ステップ = 1 セル分の長さ。
5. **孤立した環**（Node が 1 つも生じない閉路）は、行優先で最初のセルを Node とし、自己ループの Edge とする。
6. **Edge の長さ** = ステップ数 × `cellSizeMeters`。幅・速度・容量係数・輸送費は設定値（O1）。
7. **方向**: 各道路区間は既定で双方向 = 順方向・逆方向の 2 本の有向 Edge。
8. **決定性（ID 採番）**: Node は行優先（y→x）のセル順で採番。Edge は Node ID 昇順に、各 Node から 上→右→下→左 の順で辿り、同一区間は小さい Node ID 側からのみ生成して順方向 → 逆方向の順で連番を振る（自己ループは 1 区間で順・逆の 2 本）。
9. **施設の接続セル**: 道路セルでなければ設定エラー。
10. 同じ Node 対を結ぶ複数の区間は並行する別 Edge。

**閉路を含む道路構造**（上記 1〜10 を維持したうえでの補足）

実装規則: 道路セル間の**無向接続（隣り合う 2 セルの組）を一度だけ処理**し、Node から Node までの道路区間を 1 本の Edge（正逆 2 本の有向 Edge）にする。

1. **Node 確定（Edge 生成の前）**: 規則 3 の ①〜③ に該当するセルを Node とする。さらに、4 近傍で連結な道路セルの集まり（連結成分）ごとに調べ、Node が 1 つも無い成分（= 全セルが次数 2 の単純な閉路）には、成分内で行優先（y→x）に最初のセルを Node として追加する。その後、全 Node に行優先で ID を採番する。
2. **区間の辿り方**: Node ID 昇順に、各 Node から 上→右→下→左 の順で、接続された隣接セルへ向かう。その接続がまだ**未処理**なら、次数 2 のセルを辿って次の Node に着くまで進み、通過した全接続を「処理済み」にして 1 区間とする。処理済みの接続から始まる向きは飛ばす（二重処理の防止）。
3. **区間 → Edge**: 区間ごとに、辿った向きの有向 Edge、続けて逆向きの有向 Edge を連番で生成する（規則 7, 8）。長さは接続数 × `cellSizeMeters`。

ケースごとの結果:

| # | ケース | 結果 |
|---|---|---|
| a | Node が無い孤立閉路 | 手順 1 で行優先最初のセルを Node にする。その Node から出て同じ Node に戻る 1 区間 = 自己ループ（正逆 2 本） |
| b | 閉路上に施設接続セルがある | そのセルは Node（規則 3 ②）。閉路上の他の Node と合わせて区間に分割される |
| c | 閉路上にマップ外周セルがある | そのセルは Node（規則 3 ③） |
| d | 閉路上に次数 ≠ 2 のセルがある（枝・交差で出入りする） | そのセルは Node（規則 3 ①） |
| e | 閉路上に Node が複数ある | 隣り合う Node 間の道路区間ごとに独立した Edge。閉路上の Node が N 個なら N 区間 |
| f | 閉路上の Node が 1 つだけ | 閉路全体が 1 区間 = 自己ループ。規則 8 により、Node から最初に辿る向き（上→右→下→左 順で先）が順方向 |
| g | 隣り合う 2 つの Node の間に中間セルが無い | 接続 1 つの区間（長さ = 1 セル分）。接続は一度だけ処理される |
| h | 同じ Node 対を結ぶ区間が複数（例: Node が 2 つの閉路） | 区間ごとに別の Edge（並行 Edge, 規則 10）。それぞれ別の接続から始まるので混同されない |

- 自己ループや並行 Edge も、規則 8 の ID 採番（Node ID 昇順 → 向きの順）どおりに決定的に採番される。
- 道路セル間の接続は一度しか処理されないため、同じ区間が正逆の両方から重複して Edge 化されることはない。

### 4.5 tick 処理（仕様 §4 の 12 手順）

確定状態 `World` と、tick 内だけの `TickWorkspace`（計画・残容量・スナップショット・需要枠）を分ける。**World への書込みは手順 3, 9, 10, 11 のみ。** 手順 1 の `SimClock.advance()` は `Simulation` が保持する `SimClock` の更新であり、World への書き込みではない（§4.2）。量の演算はすべて ADR-003 のヘルパー経由。

| # | フェーズ | 内容 |
|---|---|---|
| 1 | 時刻更新 | `SimClock.advance()`（以降 tick 番号は advance 後の値） |
| 2 | 生産量計算 | 各生産施設: `potential = productionCapacity`（停止中は 0）。`actual = min(potential, 出力在庫の空き（tick 開始時点）, Harvester は Resource の採取可能量（tick 開始時点の current と maxExtractPerTick））`。`curtailed = potential − actual`（停止分は抑制量に含めない） |
| 3 | 在庫反映 | 生産物を出力在庫へ加算 → Resource の採取を減算 → 再生処理（ADR-010）。**ここで出荷可能量・受入側の空きのスナップショットを確定**し、発送元施設・貨物ごとの**残り出荷可能量**を在庫のスナップショット値で初期化する（TickWorkspace。World の在庫は手順 9 まで変更しない） |
| 4 | 需要生成 | ① `CONSUME`: 都市 c・貨物 g ごとに需要枠 `slot[c][g] = consumptionDemand[g]`、都市ごとに残り受入能力 = `acceptCapacity`（ADR-009）。② `FILL`: 倉庫・貨物ごとの**残り空き = min(貨物別空き, 施設全体の空き)**（スナップショット）。request ごとの `desired` は手順 8 の処理時点の残り枠/残り空き |
| 5 | 経路決定 | `RouteCache` から最小所要時間経路（ADR-004）。経路なし → 輸送量 0（未接続） |
| 6 | 容量計算 | 全 Edge の残容量 = `capacity` |
| 7 | 輸送可能量 | `limit = min(desired, 発送元の残り出荷可能量（発送元施設・貨物ごと。その時点で先行 request が使った分を除く）, 経路上の最小残容量, 受入側の残り受入能力（City）または残り空き（Warehouse））` |
| 8 | 実輸送確定 | priority 昇順 → ID 昇順の逐次処理（手順 7 の計算と交互に実行）。`shipped = limit` を確定し、経路上の全 Edge 残容量、需要枠/受入能力/残り空き、**発送元の残り出荷可能量**を減算（二重消費・在庫の二重使用なし）。輸送費を累積 |
| 9 | 出庫 | 発送元在庫を減算（発送元施設・貨物ごとの `shipped` の合計。スナップショットを超えないことを検証。手順 8 で残り出荷可能量を逐次減算しているため超えない） |
| 10 | 入庫 | 配送先在庫へ加算。都市は `consumed += shipped` |
| 11 | バックログ更新 | shortage・未輸送量を計算して記録 |
| 12 | 集計 | `TickResult`（生産・抑制・輸送・在庫・未輸送・shortage・累計・Edge 利用量）を生成 |

### 4.6 数量指標の定義（区別する）

| 指標 | 定義 | 性質 |
|---|---|---|
| 生産能力 | 設定された potential | kg/tick |
| **実生産量 `produced`** | 実際に作られた量（`actual`） | tick 別。累計 = **累計生産量** |
| **生産抑制量 `curtailed`** | `potential − actual`（出力在庫満杯・資源不足による。停止中は含まない） | tick 別 + 累計。物資ではない |
| 需要 `desired` | request 処理時点の残り需要枠 / 残り空き | 計画値 |
| **実輸送量 `shipped`** | 確定した移動量 | tick 別 + 累計 |
| **未輸送量 `untransported`** | request ごとに `untransported = min(desired, remainingSourceStock) − shipped`。`desired`（需要枠・受入能力等を反映した希望量）と `remainingSourceStock`（発送元施設・貨物の残り出荷可能量）は、いずれも**当該 request の処理開始直前の値**を使う。`shipped` はその request で確定した輸送量。手順 8 で確定して各残量を減算した後の値を式に使ってはならない（実装は処理開始直前の値を保持しておく）。発送元を共有する先行 request が在庫を割り当て済みなら、`remainingSourceStock` はその割当後の残量。発送元に物資があり需要もあったのに、次の輸送上の制約で運べなかった量: 経路未接続、道路容量不足、受入能力不足、受入側の空き容量不足、その他輸送可能量を制限する条件 | tick 別のフロー。繰り越さない。残りは発送元在庫に残る |
| **shortage** | 都市・貨物ごとに `consumptionDemand[g] − Σ shipped`（理由を問わず満たせなかった消費） | tick 別 + 累計。都市のみ |
| **累計消費量 `consumed`** | 都市が受け入れて消費した量の累計 | 物資が系外へ出る唯一の経路 |

- 発送元に物資がなく運べなかった場合は shortage にのみ現れ、未輸送量には現れない（供給不足と輸送制約を区別）。
- 経路未接続の場合は `shipped = 0` となるため、未輸送量は `min(desired, 当該 request の処理開始直前の remainingSourceStock)` になる。先行 request によって発送元在庫がすでに割り当てられている場合、後続 request ではその割当後の残量が基準となる（常にスナップショット在庫を基準とするわけではない）。発送元に物資がない（`remainingSourceStock = 0`）ために運べない量は、この式で 0 となり未輸送量に含まれず、供給不足として shortage にのみ反映される。
- 在庫バックログは別概念で、発送元に残る在庫そのもの。

### 4.7 入力検証とオーバーフロー対策

セットアップ時に検証し、違反は `InvalidConfigurationException`（理由と対象 ID を含む）。

| 項目 | 範囲 |
|---|---|
| `cellSizeMeters` | 1 ≤ 値 ≤ 10,000 |
| グリッド | `width × height` が `int` に収まる（`multiplyExact`）、各 ≥ 1 |
| `Edge.lengthM` | 1 ≤ 値 ≤ 10^9（= 1,000,000 km。セル数 × `cellSizeMeters` が範囲を超えたらエラー） |
| `Edge.speedKmh` | 1 ≤ 値 ≤ 1,000（整数。0・負は除算とコスト無限化を避けるため不可） |
| `Edge.widthM` | 1 ≤ 値 ≤ 10,000（整数 m） |
| 量（容量・能力・在庫・需要・受入能力・資源量） | 0 ≤ 値 ≤ `MAX_QUANTITY_KG` = 10^15（t → kg の乗算も `multiplyExact`）。受入能力の無制限だけ番兵 |
| `regenIntervalTicks` | ≥ 1 |
| `costPerTon` | 0 ≤ 値 ≤ 10^9 |

- 所要時間: `lengthM × 36 ≤ 3.6×10^10`、`speedKmh × 10 ≤ 10^4` なので 1 Edge の `travelSeconds` は `long` に十分収まる。経路合計・輸送費・累計値は `addExact` / `multiplyExact`。範囲検証を通っても累積で溢れた場合は ADR-003 により失敗する。
- 経路の合計コストが `Long.MAX_VALUE` に達しうる（到達不能の番兵）ことはなく、番兵は未到達ノードにのみ使い、加算前に判定する。

---

## 5. 物資保存則と各 tick の期待値

### 5.1 保存則（全 tick 終了時に検証）

1. **物資保存**: `Σ 全施設在庫 + 累計消費量 = 初期在庫合計 + 累計生産量`。輸送では物資は増減しない（出庫合計 = 入庫合計）。生産抑制量・shortage・未輸送量は保存則に含まれない。
2. **Resource 保存**: `current = 初期 current + 累計実再生量 − 累計採取量`、かつ `0 ≤ current ≤ maxAmount`。Harvester の累計生産量 = 累計採取量。
3. **非負・容量**: 全在庫 ≥ 0、`Σ amount ≤ totalCapacity`、貨物別 ≤ `cargoCapacity`。
4. **道路**: 各 Edge の tick 利用量 ≤ 容量。
5. **同 tick 再出荷なし・在庫の二重使用なし**: 各施設・貨物の出庫量（その発送元を参照する全 request の `shipped` の合計）≤ 手順 3 時点の在庫。
6. **需要枠**: 都市・貨物別の tick 入荷合計 ≤ `consumptionDemand[cargo]`、都市の貨物合計 ≤ `acceptCapacity`。
7. **オーバーフロー**: 発生したらシミュレーション失敗（失敗自体を別テストで確認）。

### 5.2 基本シナリオ（単位: t）

農場A（100 t/h、出力在庫上限 100,000 t）→ 道路1（70）→ 倉庫A（5,000 t、初期 0）→ 道路2（50）→ 都市A（農産物の需要 60、受入 80）。request A: 農場→倉庫（FILL）、B: 倉庫→都市（CONSUME）。倉庫は生産しないので tick 1 の倉庫スナップショットは 0。

| tick | 生産 | 抑制 | A輸送 | B輸送 = 都市消費 | A未輸送 | B未輸送 | shortage | 農場在庫(末) | 倉庫在庫(末) | 累計生産 | 累計消費 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 100 | 0 | 70 | 0 | 30 | 0 | 60 | 30 | 70 | 100 | 0 |
| 2 | 100 | 0 | 70 | 50 | 30 | 10 | 10 | 60 | 90 | 200 | 50 |
| 3 | 100 | 0 | 70 | 50 | 30 | 10 | 10 | 90 | 110 | 300 | 100 |
| 2 ≤ n ≤ 245 | 100 | 0 | 70 | 50 | 30 | 10 | 10 | 30n | 20n+50 | 100n | 50(n−1) |
| 246 ≤ n ≤ 2,097 | 100 | 0 | 50 | 50 | 0 | 10 | 10 | 7,350+50(n−245) | 4,950 | 100n | 50(n−1) |
| n ≥ 2,098 | 50 | 50 | 50 | 50 | 0 | 10 | 10 | 99,950 | 4,950 | 209,700+50(n−2,097) | 50(n−1) |

導出と検算:
- **倉庫の飽和**: tick n の期首倉庫在庫は 20n+30。tick 245 は期首 4,930・空き 70 → 入庫 70・出庫 50 → 末 4,950。tick 246 以降は期首 4,950・空き 50 のため A 輸送は 50（需要 = 空き 50 なので未輸送 0）。同 tick の出庫は空きに数えない（ADR-007）ため倉庫は 5,000 に達せず 4,950 で定常。
- **農場の上限到達**: 農場の期末在庫は tick 2,097 で 99,950 t（= 7,350 + 50 × 1,852）。tick 2,098 の期首空き 50 → 実生産 50、抑制 50（`curtailed = 100 − 50`）。以降、実生産 = 出荷 50 で在庫は 99,950 t に固定。
- **保存則**: tick 1: 30 + 70 + 0 = 100 ✓ / tick 2: 60 + 90 + 50 = 200 ✓ / tick 3: 90 + 110 + 100 = 300 ✓ / tick 246: 7,400 + 4,950 + 12,250 = 24,600 ✓ / tick 2,098: 99,950 + 4,950 + 104,850 = 209,750 = 209,700 + 50 ✓。
- tick 1 の B 未輸送が 0 で shortage が 60 なのは供給不足（倉庫に物資がない）。tick 2 以降は道路 2（50）が制約で、都市の受入能力 80 は制約にならない。
- 需要枠の確認（ADR-009）: request が 1 本のため `desired = 60`。別テストで同一都市・同一貨物に 2 本（priority 1: 50、priority 2: 道路に余裕あり）の request を置き、入荷合計が需要 60 を超えないこと、後順位が枠の残りしか受けないことを検証する。

---

## 6. テスト計画

- **単体**: `Quantity`（オーバーフロー検出）、`SimClock`（番号規則）、`Stock`（貨物別/全体容量・空き）、`Resource`（tick 開始 current での採取、採取後再生、最初の再生 tick = 周期、上限）、`RoadGraphBuilder`（§4.4: 次数・曲がり角・環・外周・施設接続・ID 決定性・一方通行・2×2 塊のエラー・斜めのみ接続の警告・閉路（Node 無し閉路、閉路上の施設/外周/分岐セル、複数 Node、Node 1 つの自己ループ、隣り合う Node、並行 Edge、接続の二重処理なし）、`RouteFinder`（所要時間最小、**同コスト時の Edge ID タイブレーク**、並行 Edge、未接続）、`FlowPlanner`（優先度順・同一道路共有・**同一都市・同一貨物に複数 request**・FILL の min(貨物別空き, 全体空き)）、入力検証（§4.7 の境界値と範囲外）。
- **シナリオ**（仕様 §6）: 基本 + 追加ケース 1〜9。道路容量大/小、生産能力不足、倉庫容量不足、共有道路、未接続、停止、長期（10 年 = 87,600 tick。§5.2 の飽和・抑制を通過する）、同一設定の結果完全一致（`TickResult` 列を比較）。
- **全 tick で検証する不変条件**: §5.1 の 1〜6。
- **失敗系**: 容量や在庫を `MAX_QUANTITY_KG` 付近に設定して累積オーバーフロー → `SimulationFailedException`、以後の `step()` 拒否。
- **依存方向**: `com.uchicom.mou.**`（`support` を除く）が `com.uchicom.pj` / `com.uchicom.util` / `com.uchicom.zouni` などをインポートしていないことを検証する JUnit テスト。
- **発送元在庫の二重使用防止（ADR-002）の追加テスト**: 同一発送元・同一貨物から、別々の配送先（City A, City B）へ 2 本の request を出す。発送元在庫 80 t（スナップショット）、Request A: 需要 60 t・priority 1（宛先 City A）、Request B: 需要 60 t・priority 2（宛先 City B）、それぞれの経路容量は 60 t 以上（受入能力も十分）。発送元在庫を priority 順に逐次配分する。

  | 項目 | 期待値 |
  |---|---:|
  | Request A shipped | 60 t |
  | Request B shipped | 20 t |
  | Request A untransported | 0 t |
  | Request B untransported | 0 t |
  | City A shortage | 0 t |
  | City B shortage | 40 t |
  | 発送元の期末在庫 | 0 t |

  Request B の未輸送量が 0 t なのは、処理開始直前の `remainingSourceStock` が 20 t であり（`min(60, 20) − 20 = 0`）、その 20 t をすべて輸送できたため。一方、City B は需要 60 t に対し供給が 20 t のため shortage は 40 t（供給不足は shortage にのみ反映）。
  このテストは、発送元在庫の逐次配分、request 単位の untransported、都市・貨物単位の shortage が区別されることを検証する。全 tick の不変条件（§5.1 の 5）でも確認する。
- **Maven 依存関係**: 次を受け入れ条件とし、`mvn verify`（CI を含む）で自動検証する。具体的な方法・プラグインは Issue #1 で選定する。
  - mou のコアは JDK 以外の runtime 依存（compile / runtime スコープ）を持たない。
  - JUnit と AssertJ は test スコープの依存として許容する。
  - anko 由来のライブラリ（iciql、H2、HikariCP、java-jwt、hibernate-validator、jakarta.el、jackson、zouni、uchicom `util`、mockito など）や、その他の外部ライブラリが runtime 依存に残っていない。
  - 上記の「依存方向」のインポート検査は維持する（Maven 依存検証はそれを置き換えず、補う）。
- **Demand Slot（ADR-009）の追加テスト**: 先行 request が道路容量で制限されたとき、後続 request が残りの需要枠を使えることを検証する。条件: 都市の同一貨物の需要 = 60 t/tick（受入能力は 60 t 以上）。request A と B は別の経路・別の道路を使う。

  | Request | Priority | 発送可能量 | 経路容量 |
  |---|---:|---:|---:|
  | A | 1 | 100 t | 30 t |
  | B | 2 | 100 t | 100 t |

  期待結果: A の輸送量 30 t（`desired` = 60 → 経路容量 30 で制限）、B の輸送量 30 t（`desired` = 残り枠 30）、都市への合計入荷量 60 t、shortage 0 t、A の未輸送量 30 t（`min(60, 100) − 30`）、B の未輸送量 0 t（`min(30, 100) − 30`）。
  このテストは、**未輸送量が request 単位、shortage が都市・貨物単位の指標**であり、両者が一致しないことがある（ここでは A の未輸送量 30 t があっても shortage は 0）ことも確認する。
- **カバレッジ**: JaCoCo の check ルールを `network / logistics / facility / sim` に付け、行・分岐 100% を目標にする。

---

## 7. 実装タスク（GitHub Issue 案）

各 Issue は完了時にテスト結果をコメントへ記録する。

| # | Issue | 受け入れ条件 | 依存 |
|---|---|---|---|
| 1 | プロジェクト基盤・anko 資産の整理 | §8 の一覧に従い、再利用物は mou 用に修正して残し、不要物を削除。`mvn verify` 成功、依存方向テスト、Maven 依存関係の検証（§6）、`CLAUDE.md`/README/CI を mou 用に更新 | O4 の承認 |
| 2 | Quantity・SimClock・SimulationConfig | exact 演算とオーバーフロー例外、1 日 = 24 / 1 年 = 8,760 tick、tick 番号規則、t/h → kg/tick、§4.7 の検証 | 1 |
| 3 | GridMap・SeedMap・BitmapSeedLoader・施設定義読込 | 色 → CellType（設定可）、1px = 1 セル、cellSize、`.properties` による座標指定の施設定義、土地利用の保持 | 2, O2 |
| 4 | RoadGraphBuilder・RoadNetwork・RouteFinder | §4.4 の全規則（2×2 塊エラー含む）、有向 Edge・一方通行、ADR-004 の Dijkstra、キャッシュ | 3, O1 |
| 5 | Stock・Resource・CargoType | §4.3 の定義、ADR-010、保存則テスト | 2 |
| 6 | Facility（Farm/Harvester/Warehouse/City） | 実生産・生産抑制、停止、都市の受入、需要設定 | 5 |
| 7 | FlowPlanner（手順 4〜11） | 逐次 greedy、需要枠（ADR-009）、スナップショット規則、FILL 需要量、未輸送量・shortage | 4, 6 |
| 8 | Simulation ループ・TickResult | 12 フェーズ、複数 tick、FAILED 状態、累計値 | 7 |
| 9 | LinearChainScenario と仕様 §6 のテスト | §5.2 の表と一致（tick 1〜3, 246, 2,098 含む）、追加ケース 1〜9、不変条件の全 tick 検証 | 8 |
| 10 | コンソール / CSV 出力 | tick ごとの生産・抑制・輸送・在庫・未輸送・shortage・累計 | 8 |
| 11 | 実装状況一覧・ADR 整備 | 実装済み/未実装の一覧、ADR の最終化 | 9, 10 |

---

## 8. anko 資産の再利用方針（Q0）

調査結果（コード・設定の読解による。実行・変更はしていない）。**分類は提案であり、承認後に Issue #1 で実施する（O4）。**

### 8.1 再利用可（そのまま）
- コンパイラ設定（JDK 25、Error Prone、`--add-exports/opens`）、Spotless（google-java-format、ライセンスヘッダ、sortPom、Tab 整形）、JaCoCo の `prepare-agent` / `report`、release-plugin。
- CI: `verify.yml`、`package.yml`（reviewdog による Error Prone の PR レビュー）、dependabot。
- `.claude/skills/fix-issue`（プロジェクト非依存）。

### 8.2 修正すれば再利用可
| 対象 | 修正内容 |
|---|---|
| JaCoCo の check ルール | 対象を `com.uchicom.pj.*` から mou のパッケージ（§6）へ差し替え |
| surefire | mockito の javaagent と存在しない `logging.properties` の参照を除去 |
| exec-maven-plugin / assembly | `mainClass` を mou の Main に。依存がないため通常 jar + manifest で足りる |
| `release.yml` | 旧名称（`memo`）の成果物名を mou に |
| `.devcontainer` | ポート 8080・H2 パス・名称（zouni）を除去 |
| `DailyRollingFileHandler`（JDK の `java.util.logging` のみ使用） | `Constants.LOG_DIR` と Asia/Tokyo 固定への依存を除去。MVP のコンソール出力には不要のため、必要になった時点で `support` パッケージに取り込む。**コアのパッケージからは参照しない** |
| `CLAUDE.md` / `AGENTS.md` / `README.md` | mou 用に書き換え |

### 8.3 不要
- 依存ライブラリ: iciql、H2、HikariCP、java-jwt、hibernate-validator、jakarta.el、jackson、zouni、uchicom `util`、mockito（コアは JDK のみ。設定読込は `Properties`、CSV は JDK で足りる）。
- コード: `servlet` / `api` / `dao` / `entity` / `dto` / `annotation` / `service` / `factory` / `Context` / `Config` / `Constants` / `Main`（zouni 起動）/ `ClassUtil` / `SecurityUtil` / `ValidateUtil` / `AbstractDb` / `ThrowingBiFunction` / `ViolationException`、およびそれらのテスト。
- `www/`、`database/`、`src/main/resources/{config,db}.properties`。

### 8.4 コアに再利用できる設計・実装
なし。anko は業務 Web アプリ基盤で、mou のドメインに対応する実装がない。流用するのはテスト規約（JUnit + AssertJ）と Error Prone の運用方針のみ。

### 8.5 メリット・デメリット
- メリット: 品質ゲート（静的解析・整形・カバレッジ・CI）をそのまま使え、立ち上げが速い。ログハンドラは必要時に使える。
- デメリット: 設定の修正漏れで anko 固有の参照が残る恐れがある（§6 の依存方向テストで防ぐ）。anko の履歴を残したままだとリポジトリの目的が曖昧になる。

---

## 9. 変更一覧

### 9.2 Rev.5・Rev.6 変更一覧（Rev.4 → Rev.5 → Rev.6）

Rev.5: 発送元在庫の二重使用の防止に限定した補正。Rev.6: Rev.5 の軽微な不整合の修正（未輸送量の値の参照タイミング、旧注記、テストの shortage 期待値）。基本シナリオ（§5.2）は発送元を共有する request を含まないため、**期待値・数値は変更なし**。

**Rev.6 変更（Rev.5 → Rev.6）**

| # | 変更箇所 | 変更内容 | 理由 |
|---|---|---|---|
| 6-1 | §4.6 未輸送量 | 式 `untransported = min(desired, remainingSourceStock) − shipped` の `desired`・`remainingSourceStock` は当該 request の処理開始直前の値であり、手順 8 で確定・減算した後の値を使ってはならないと明記 | 逐次配分と未輸送量の関係の明確化 |
| 6-2 | §4.6 注記 | 経路未接続時の旧注記（常にスナップショット在庫を基準とする説明）を削除し、`min(desired, 処理開始直前の remainingSourceStock)` と、先行 request の割当後の残量が基準になる旨に修正。供給不足は未輸送量に含めない定義は維持 | Rev.5 の不整合の解消 |
| 6-3 | §6 発送元在庫の追加テスト | 条件（別々の配送先 City A / City B、在庫 80 t、需要 60 t × 2、priority 1 / 2、経路容量 60 t 以上）を明記し、期待値（shipped 60 / 20、untransported 0 / 0、shortage City A 0 / City B 40、発送元の期末在庫 0）と理由を追記 | 逐次配分・request 単位の untransported・都市・貨物単位の shortage の区別を検証するため |
| — | 冒頭・§9.2 | Rev.6 に更新、変更一覧に Rev.6 を追記 | 変更点の一覧化 |

**Rev.5 変更（Rev.4 → Rev.5）**

| # | 変更箇所 | 変更内容 | 理由 |
|---|---|---|---|
| 5-1 | ADR-002 | 発送元施設・貨物ごとの「残り出荷可能量」を request ごとに逐次減算し、在庫の二重使用を禁止すると追記 | 複数 request が同じ在庫を重複して割り当てる問題への対応 |
| 5-2 | §4.5 手順 3 | スナップショット確定時に、残り出荷可能量を在庫スナップショットで初期化（TickWorkspace）と追記 | 同上 |
| 5-3 | §4.5 手順 7 | 上限に使う値を「発送元スナップショット在庫」から「発送元の残り出荷可能量」へ変更 | 同上 |
| 5-4 | §4.5 手順 8 | 確定した `shipped` を発送元の残り出荷可能量からも減算 | 手順 7 の前提（指示では未記載だが、手順 7 の変更に必須のため追加） |
| 5-5 | §4.5 手順 9 | 検証の説明を発送元施設・貨物ごとの合計に明確化 | 整合 |
| 5-6 | §4.6 未輸送量 | 式中の「発送元スナップショット在庫」を「発送元の残り出荷可能量」に変更（発送元を共有しなければ従来と同値）。**この式の変更は指示の本文が途切れていたため、手順 7 との整合のために私が加えたもの。要確認** | 手順 7 との整合（先行 request が使った在庫は後続の「物資がある」に数えない） |
| 5-7 | §5.1 の 5 | 不変条件を「発送元を参照する全 request の出庫合計 ≤ スナップショット在庫」に明確化 | 検証可能にするため |
| 5-8 | §6 | 発送元在庫の二重使用防止のテスト（在庫 80 t、要求 60 t × 2 → 60 / 20）を追加 | 検証 |
| — | 冒頭・§9 | Rev.5 に更新、変更一覧を 9.1（Rev.4）と 9.2（Rev.5）に分割 | 変更点の一覧化 |

**実施していないこと**: Java コードの実装・変更・削除・移動、Maven 設定・CI・GitHub Issue の実行・変更。変更対象は本ファイルのみ。

### 9.1 Rev.4 変更一覧（Rev.3 → Rev.4）

Rev.3 の全章・決定事項・ADR・パッケージ構成・Issue #1〜#11 の構成は維持し、指定箇所のみ追記・修正した。既存の数値・計算式・基本シナリオの期待値（§5）は変更していない。

| # | 変更箇所 | 変更内容 | 理由 |
|---|---|---|---|
| 2.1 | ADR-005 | 未輸送量に含める制約（経路未接続・道路容量不足・受入能力不足・受入側の空き容量不足・その他）を明記。発送元に物資がない場合は含めず shortage にのみ反映と追記 | 未輸送量と shortage の区別を曖昧にしないため |
| 2.1 | §4.6 表・注記 | 同様に説明を補足。経路未接続時は `shipped = 0` で全量が未輸送量になる旨を注記。**定義式は不変** | 同上 |
| 2.2 | §4.1 | `Simulation` が `SimClock` を保持、`World` は時刻を持たないと注記 | 責務の明確化 |
| 2.2 | §4.2 | 責務分担を追記（`SimClock` = 進行時刻、`World` = 可変状態で時刻進行の責務なし、tick 番号は `Simulation` が値で渡す）。tick 番号規則（開始時 0、最初の処理 tick は 1）は不変 | 責務の明確化 |
| 2.2 | §4.5 | 手順 1 の `SimClock.advance()` は World への書き込みではないと注記。「World への書込みは手順 3, 9, 10, 11 のみ」は維持 | 書込み規則と時刻管理の矛盾回避 |
| 2.3 | ADR-006 | 閉路を含む道路構造の扱いを §4.4 に定義した旨を追記 | 閉路処理の明確化 |
| 2.3 | §4.4 | 「閉路を含む道路構造」を追加。実装規則（Node 確定 → 無向接続を一度だけ処理して区間を Edge 化）と 8 ケース（孤立閉路、閉路上の施設/外周/分岐セル、複数 Node、Node 1 つの自己ループ、隣り合う Node、並行 Edge）を定義。規則 1〜10、ID 決定性、正逆 2 本の生成、自己ループの扱いは不変 | 実装可能な粒度にするため |
| 2.3 | §6 単体テスト | RoadGraphBuilder のテスト項目に閉路ケースを追記 | §4.4 の検証 |
| 2.4 | §6 | Maven 依存関係の検証を追加（コアは JDK 以外の runtime 依存なし、JUnit / AssertJ は test スコープで許容、anko 由来・外部ライブラリが runtime に残っていないこと）。方法・プラグインは Issue #1 で選定。既存の依存方向テストは維持 | 依存の混入防止 |
| 2.4 | §7 Issue #1 | 受け入れ条件に「Maven 依存関係の検証（§6）」を追記（Issue 構成は不変） | §6 との整合 |
| 2.5 | §6 | Demand Slot の追加テストを追記（需要 60 t、A: P1 / 100 t / 30 t、B: P2 / 100 t / 100 t → 輸送 30 / 30、合計 60、shortage 0、未輸送 30 / 0）。未輸送量は request 単位、shortage は都市・貨物単位であることを確認 | ADR-009 の検証 |
| — | 冒頭 | タイトルを Rev.4 に、状態に「設計書のみの変更」を明記、Rev.3 からの変更概要を追記 | 変更範囲の明示 |
| — | §9 | 本変更一覧 | 変更点の一覧化 |

**実施していないこと**: Java コードの実装・変更・削除・移動、Maven 設定・CI・GitHub Issue の実行・変更。変更対象は本ファイル（`docs/design.md`）のみ。
