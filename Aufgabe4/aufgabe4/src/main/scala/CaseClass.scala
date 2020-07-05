case class Count(text : String)
case class Line(docLine : String)
case class FilePath(path : String)
case class Word(word: String)
case class CountPart(value : Int)
case class OutputCount(count : Int)

// domain model
final case class Item(name: String, frequency: Long)
final case class Order(items: List[Item])