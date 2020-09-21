/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.util.np

import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class LinspaceSpec extends AnyFunSpec with Matchers {

  val pyArr =
    Array(0d, 0.390625, 0.78125, 1.171875, 1.5625, 1.953125,
      2.34375, 2.734375, 3.125, 3.515625, 3.90625, 4.296875,
      4.6875, 5.078125, 5.46875, 5.859375, 6.25, 6.640625,
      7.03125, 7.421875, 7.8125, 8.203125, 8.59375, 8.984375,
      9.375, 9.765625, 10.15625, 10.546875, 10.9375, 11.328125,
      11.71875, 12.109375, 12.5, 12.890625, 13.28125, 13.671875,
      14.0625, 14.453125, 14.84375, 15.234375, 15.625, 16.015625,
      16.40625, 16.796875, 17.1875, 17.578125, 17.96875, 18.359375,
      18.75, 19.140625, 19.53125, 19.921875, 20.3125, 20.703125,
      21.09375, 21.484375, 21.875, 22.265625, 22.65625, 23.046875,
      23.4375, 23.828125, 24.21875, 24.609375, 25d, 25.390625,
      25.78125, 26.171875, 26.5625, 26.953125, 27.34375, 27.734375,
      28.125, 28.515625, 28.90625, 29.296875, 29.6875, 30.078125,
      30.46875, 30.859375, 31.25, 31.640625, 32.03125, 32.421875,
      32.8125, 33.203125, 33.59375, 33.984375, 34.375, 34.765625,
      35.15625, 35.546875, 35.9375, 36.328125, 36.71875, 37.109375,
      37.5, 37.890625, 38.28125, 38.671875, 39.0625, 39.453125,
      39.84375, 40.234375, 40.625, 41.015625, 41.40625, 41.796875,
      42.1875, 42.578125, 42.96875, 43.359375, 43.75, 44.140625,
      44.53125, 44.921875, 45.3125, 45.703125, 46.09375, 46.484375,
      46.875, 47.265625, 47.65625, 48.046875, 48.4375, 48.828125,
      49.21875, 49.609375, 50d, 50.390625, 50.78125, 51.171875,
      51.5625, 51.953125, 52.34375, 52.734375, 53.125, 53.515625,
      53.90625, 54.296875, 54.6875, 55.078125, 55.46875, 55.859375,
      56.25, 56.640625, 57.03125, 57.421875, 57.8125, 58.203125,
      58.59375, 58.984375, 59.375, 59.765625, 60.15625, 60.546875,
      60.9375, 61.328125, 61.71875, 62.109375, 62.5, 62.890625,
      63.28125, 63.671875, 64.0625, 64.453125, 64.84375, 65.234375,
      65.625, 66.015625, 66.40625, 66.796875, 67.1875, 67.578125,
      67.96875, 68.359375, 68.75, 69.140625, 69.53125, 69.921875,
      70.3125, 70.703125, 71.09375, 71.484375, 71.875, 72.265625,
      72.65625, 73.046875, 73.4375, 73.828125, 74.21875, 74.609375,
      75d, 75.390625, 75.78125, 76.171875, 76.5625, 76.953125,
      77.34375, 77.734375, 78.125, 78.515625, 78.90625, 79.296875,
      79.6875, 80.078125, 80.46875, 80.859375, 81.25, 81.640625,
      82.03125, 82.421875, 82.8125, 83.203125, 83.59375, 83.984375,
      84.375, 84.765625, 85.15625, 85.546875, 85.9375, 86.328125,
      86.71875, 87.109375, 87.5, 87.890625, 88.28125, 88.671875,
      89.0625, 89.453125, 89.84375, 90.234375, 90.625, 91.015625,
      91.40625, 91.796875, 92.1875, 92.578125, 92.96875, 93.359375,
      93.75, 94.140625, 94.53125, 94.921875, 95.3125, 95.703125,
      96.09375, 96.484375, 96.875, 97.265625, 97.65625, 98.046875,
      98.4375, 98.828125, 99.21875, 99.609375
    )

  describe("linspace should behave like numpy.linspace") {
    it("linspace [0; 100) with 255 points") {
      val actual = linspace(0, 100, 256) // should contain theSameElementsAs pyArr
      cfor(0)(_ < actual.length, _ + 1) { i =>
        actual(i) shouldBe (pyArr(i) +- 4e-1) // math differs a bit from the py math
      }
    }
  }
}