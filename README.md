# ExpandableTextView
支持文字展开和收起的TextView, 继承TextView实现, 可以支持文字或图片作为展开或折叠的按钮
实现思路基本上是先获取一份用户输入的文字，在onMeasure时计算出折叠或展开时的内容和高度以及对应图标的位置，在onDraw时绘制折叠和展开的图标, 具体实现方式见代码onMeasure, onDraw和onTouchEvent

为什么使用paint去绘制按钮而不是使用ImageSpan?
因为汉字英文数字在某些组合排列的情况下会导致ImageSpan被换到下一行, 处理起来太麻烦了, 不如自己去做

![屏幕截图](doc/screencap.png)
[使用方法](app/src/main/java/com/jiaoay/expandabletextview/MainActivity.kt)
