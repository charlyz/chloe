package net.chloe.models

case class Color(
  integerRepresentation: Int
) {
  
  val red = (integerRepresentation & 0xff0000) / 65536
  val green = (integerRepresentation & 0xff00) / 256
  val blue = (integerRepresentation & 0xff)
  
  require(red >= 0)
  require(red <= 255)
  require(green >= 0)
  require(green <= 255)
  require(blue >= 0)
  require(blue <= 255)
    
  def isRed =  red == 255 && green == 0 && blue == 0
  
  def isGreen = red == 0 && green == 255 && blue == 0
  
  def isBlue = red == 0 && green == 0 && blue == 255
  
  def convertColorIntensityToPercent(colorIntensity: Int) = {
    ((colorIntensity.toFloat / 255) * 100).round
  }
  
  def convertColorIntensityToBoolean(colorIntensity: Int) = {
    colorIntensity / 255 == 1
  }
  
  def getRedAsPercentage = convertColorIntensityToPercent(red)
  def getGreenAsPercentage = convertColorIntensityToPercent(green)
  def getBlueAsPercentage = convertColorIntensityToPercent(blue)
  
  def getRedAsBoolean = convertColorIntensityToBoolean(red)
  def getGreenAsBoolean = convertColorIntensityToBoolean(green)
  def getBlueAsBoolean = convertColorIntensityToBoolean(blue)
  
  override def equals(obj: Any) = {
    val color = obj.asInstanceOf[Color]
    color.red == red && color.green == green && color.blue == blue 
  }
  
}
