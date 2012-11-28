package geotrellis.raster.op.focal

import scala.math.{min,max}
import scala.collection.mutable.LinkedList

import Movement._

/*
 * Represents a mask over a cursor. The CursorMask
 * helps the cursor keep track of the state of masking
 * and unmasking of cells between moves.
 */
class CursorMask(d:Int,f:(Int,Int)=>Boolean) {
  private class MaskSet {
    private var data = Array.ofDim[LinkedList[Int]](d)
    for(i <- 0 to d-1) { data(i) = new LinkedList[Int]() }

    def add(i:Int,v:Int) = { data(i) = data(i) :+ v }

    def foreachAt(i:Int)(f:Int=>Unit) = {
      data(i).foreach(v => f(v))
    }

    def apply(i:Int) = data(i)
  }
  
  // Holds data about what cells are unmasked
  private val unmasked = new MaskSet
  private var westColumnUnmasked = new LinkedList[Int]
  private var eastColumnUnmasked = new LinkedList[Int]

  // Holds data about what cells are masked or unmasked by moving the cursor.
  private val maskedAfterMoveUp = new MaskSet
  private val unmaskedAfterMoveUp = new MaskSet
  private val maskedAfterMoveLeft = new MaskSet
  private val unmaskedAfterMoveLeft = new MaskSet

  // Record the mask values
  var x = 0
  var y = 0
  var len = 0
  var isMasked = false
  var leftIsMasked = false
  while(y < d) {
    x = 0
    len = 0
    while(x < d) {
      isMasked = f(x,y)
      if(!isMasked) {
        unmasked.add(y,x)

        // Record the border columns, they are a special case to be tracked.
        if(x == 0) {
          westColumnUnmasked = westColumnUnmasked :+ y
        }
        if(x == d - 1) {
          eastColumnUnmasked = eastColumnUnmasked :+ y
        }
      }

      // Check if moving left yeilds a different mask result,
      // so we can tell if moving horizontally has changed the
      // masked value for a cell.
      if(x > 0) {
        val leftIsMasked = f(x-1,y)

        if(leftIsMasked && !isMasked) {
          // Moving left will unmask
          unmaskedAfterMoveLeft.add(y,x)
        } else if(!leftIsMasked && isMasked) {
          // Moving left will mask
          maskedAfterMoveLeft.add(y,x)
        }
      }
      
      //Check if moving up yeilds different mask result
      if(y > 0) {
        val upIsMasked = f(x,y-1)
        
        if(upIsMasked && !isMasked) {
          // Moving up will unmask
          unmaskedAfterMoveUp.add(y,x)
        } else if(!upIsMasked && isMasked) {
          // Moving up will mask
          maskedAfterMoveUp.add(y,x)
        }
      }
      x += 1
    }
    y += 1
  }
 
  def foreachX(row:Int)(f:Int=>Unit) = {
    unmasked.foreachAt(row)(f)
  }
 
  def foreachWestColumn(f:Int=>Unit) = {
    westColumnUnmasked.foreach(f)
  }

  def foreachEastColumn(f:Int=>Unit) = {
    eastColumnUnmasked.foreach(f)
  }

  private def foreach(xOffset:Int,yOffset:Int,startY:Int,set:MaskSet)(f:(Int,Int)=>Unit) = {
    var y = startY
    while(y < d) {
      set.foreachAt(y) { x => f(x - xOffset, y - yOffset) }
      y += 1
    }
  }

  def foreachMasked(mv:Movement)(f:(Int,Int)=>Unit) {
    mv match {
      case Left => foreach(0,0,0,maskedAfterMoveLeft)(f)
      case Right => foreach(1,0,0,unmaskedAfterMoveLeft)(f)
      case Up => foreach(0,0,1,maskedAfterMoveUp)(f)
      case Down => foreach(0,1,1,unmaskedAfterMoveUp)(f)
      case _ =>
    }
  }

  def foreachUnmasked(mv:Movement)(f:(Int,Int)=>Unit) {
    mv match {
      case Left => foreach(0,0,0,unmaskedAfterMoveLeft)(f)
      case Right => foreach(1,0,0,maskedAfterMoveLeft)(f)
      case Up => foreach(0,0,1,unmaskedAfterMoveUp)(f)
      case Down => foreach(0,1,1,maskedAfterMoveUp)(f)
      case _ =>
    }
  }

  def asciiDraw:String = {
    var r = ""
    for(y <- 0 to d - 1) {
      for(x <- 0 to d - 1) {
        if(unmasked(y).contains(x)) { r += " O " } else { r += " X " }
      }
      r += "\n"
    }
    r
  }
}
