# Page — Job Sources

**布局：上下垂直**（非左右分栏）。

1. 顶部：LinkedIn 风格**大搜索框** + 保存；绑定当前选中 Source（默认 freehire）。  
2. 下方：Source 卡片纵向排列（enabled、riskNote、上次 run）。  
3. 再下：Run 历史纵向列表；点选看 diagnostics。  
4. 同步按钮：无 searchTerms → 展示错误，不发或捕获 400。  
5. 填过并 PATCH 成功后，定时任务用已存词，无需再绑 Profile。
