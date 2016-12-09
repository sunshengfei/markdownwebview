### MarkDownWebView for Android
> 本模块提取自个人另外一个博客的android项目，实现将markdown格式文本以webview解析显示。

### 说明

1. 大多数情况下，使用 `TextView` 配合 `Spanned`就可以实现，由于`表格`实现太费劲（可以使用`制表符`拼出来，也可以使用Canvas画出来），加上语法高亮什么的，还是采用webview好了。

2. 由于jQuery或jQuery.Mobile版js过于庞大（拖累加载速度），未采用需要jquery的BootStrap-markdown.js，而采用了marked.js+highlight.js解析并展示。

### 使用方法很简单

```
mMarkDownWebView.setContent("### Hello World ");

```
仅做分享，按需求扩展。





意见或建议请Email：[Gsiner@live.com](mailto:Gsiner@live.com)

个人PHP博客网站：[【个人网站】](http://www.freddon.com/)

个人PHP博客项目：开发中

O(∩_∩)O谢谢