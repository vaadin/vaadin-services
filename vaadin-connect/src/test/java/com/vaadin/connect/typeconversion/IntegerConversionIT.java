/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.connect.typeconversion;

import org.junit.Test;

public class IntegerConversionIT extends BaseTypeConversion {

  @Test
  public void should_ConvertNumberToInt_When_ReceiveNumberAsNumber() {
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "1", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "0", "1");
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "-1", "0");

    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "1", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "0", "1");
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "-1", "0");
  }

  @Test
  public void should_ConvertNumberToInt_When_ReceiveNumberAsString() {
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "\"1\"", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "\"0\"", "1");
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "\"-1\"", "0");

    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "\"1\"", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "\"0\"", "1");
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "\"-1\"", "0");
  }

  @Test
  public void should_HandleOverflowInteger_When_ReceiveOverflowNumber() {
    String overflowInputInteger = "2147483648";
    assertEqualExpectedValueWhenCallingMethod("addOneInt", overflowInputInteger,
        String.valueOf(Integer.MIN_VALUE + 1));
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", overflowInputInteger,
        String.valueOf(Integer.MIN_VALUE + 1));

    String underflowInputInteger = "-2147483649";
    // underflow will become MAX, then +1 in the method => MIN
    assertEqualExpectedValueWhenCallingMethod("addOneInt", underflowInputInteger,
        String.valueOf(Integer.MIN_VALUE));
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", underflowInputInteger,
        String.valueOf(Integer.MIN_VALUE));
  }

  @Test
  public void should_ConvertDecimalToInt_When_ReceiveADecimalAsNumber() {
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "1.1", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "-1.9", "0");

    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "1.1", "2");
    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "-1.9", "0");
  }

  @Test
  public void should_FailToConvertDecimalToInt_When_ReceiveADecimalAsString() {
    assert400ResponseWhenCallingMethod("addOneInt", "\"1.1\"");

    assert400ResponseWhenCallingMethod("addOneIntBoxed", "\"1.1\"");
  }

  @Test
  public void should_HandleSpecialInputForInt_When_ReceiveNull() {
    assertEqualExpectedValueWhenCallingMethod("addOneInt", "null", "1");

    assertEqualExpectedValueWhenCallingMethod("addOneIntBoxed", "null", "null");
  }

  @Test
  public void should_HandleSpecialInputForInt_When_ReceiveSpecialInput() {
    assert400ResponseWhenCallingMethod("addOneInt", "NaN");

    assert400ResponseWhenCallingMethod("addOneIntBoxed", "NaN");
  }
}
