package GoooBigTestCases;

import GoooBigBase.TestBase;
import GoooBigScreens.AdditionForProductScreen;
import io.qameta.allure.Allure;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoooBig_addFirstProductWithAddition extends TestBase {

    public String totalAmount;
    AdditionForProductScreen additionForProductScreen;

    @Test(priority = 1)
    public void addFirstProductWithAddition() throws InterruptedException, IOException {
        additionForProductScreen = new AdditionForProductScreen();
        additionForProductScreen.addProduct();
        additionForProductScreen.hideKeyboard();

        additionForProductScreen.goToCart();
        Thread.sleep(2000);
        Allure.addAttachment("Screenshot for Cart", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        additionForProductScreen.clickOnNextBasket();
        Thread.sleep(3000);
        Allure.step("verify to get data like price and Tax an total price of the invoice.");
        String details = additionForProductScreen.getDetailsOfInvoice();

        System.out.println("بيانات الفاتورة الموجودة فى السلة");
        System.out.println("----------------------------------");
        // Define the regular expression pattern to match text and numbers
        Pattern pattern = Pattern.compile("([\\p{InArabic}\\s]+)|(SR \\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(details);
        // Skip initial texts if needed
        boolean skipInitialTexts = true;
        String text = " " + "إجمالى الضريبة";
        String lastText = "";
        List<String> numbers = new ArrayList<>();
        // Iterate through the matches and print the text and number
        while (matcher.find()) {
            String group = matcher.group();
            // Skip initial texts if needed
            if (skipInitialTexts) {
                if (group.contains("إجمالى الضريبة")) {
                    skipInitialTexts = false;
                }
                continue;
            }
            if (group.contains("SR")) {
                String number = group.substring(group.indexOf("SR") + 3); // Extract the number after "SR"
                numbers.add(number);
                lastText = text;
                System.out.println(text + number);
                Allure.addAttachment("Test Output", "text/plain", "Invoice Details Are: " + text + number);
            } else {
                text = group;
            }
        }
        // Print the last text and number
        if (!lastText.isEmpty()) {
            System.out.println(lastText + numbers.get(numbers.size() - 1));
            Allure.addAttachment("Test Output", "text/plain", "Invoice Details Are: " + lastText + numbers.get(numbers.size() - 1));
        }
        System.out.println("----------------------------------");
        Allure.step("verify to do payment and print the invoice.");
        additionForProductScreen.clickOnCashButton();
        Thread.sleep(4000);
        // Capture the screenshot and save it to a file
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File screenshotFile = new File("invoice_screenshot.png");
        FileUtils.copyFile(screenshot, screenshotFile);
        // Use Tesseract to extract text from the image
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("Arabic"); // Set the language to Arabic
        String extractedText;
        try {
            extractedText = tesseract.doOCR(screenshotFile);
            System.out.println("البيانات المستخرجه من الفاتورة كلها");
            System.out.println("----------------------------------");
            System.out.println(extractedText);
            Allure.addAttachment("Extracted Invoice Text", "text/plain", extractedText);
            // Filter the extracted text
            String[] lines = extractedText.split("\n");
            StringBuilder filteredText = new StringBuilder();
            // Patterns to match the lines of interest
            String[] keywords = {
                    "الخصم",
                    "إجمالى المبلغ المستحق"
            };
            for (String line : lines) {
                for (String keyword : keywords) {
                    if (line.contains(keyword)) {
                        // Remove intermediate numbers
                        line = line.replaceAll("\\s+\\d+/\\d+\\s+", " ");
                        line = line.replaceAll("\\s+\\d+\\s+", " ");
                        filteredText.append(line).append("\n");
                        break;
                    }
                }
            }
            // Remove the unwanted line if it appears in the filtered text
            String result = filteredText.toString().replaceAll("كاش \\d+\\s*", "").trim();
            System.out.println("----------------------------------");
            System.out.println("البيانات المطلوبة من الفاتورة");
            System.out.println("----------------------------------");
            System.out.println(result);
            Allure.addAttachment("Filtered Invoice Text", "text/plain", result);
            // Extract values by key
            totalAmount = extractValueByKey(result, "إجمالى المبلغ المستحق");
            // Accessing the first, second, and third numbers
            System.out.println("البيانات المطلوبة من السلة");
            System.out.println("----------------------------------");
            if (numbers.size() >= 3) {
                String firstNumber = numbers.get(0);
                String secondNumber = numbers.get(1);
                String thirdNumber = numbers.get(2);
                System.out.println("إجمالى الضريبة(السلة) : " + firstNumber);
                System.out.println("الخصم(السلة) : " + secondNumber);
                System.out.println("المجموع(السلة) : " + thirdNumber);
                // Convert strings to BigDecimal for comparison
                BigDecimal expectedTotalAmount = new BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP);
                BigDecimal actualTotalAmount = new BigDecimal(thirdNumber).setScale(2, RoundingMode.HALF_UP);
                // Assertions
                Allure.step("Assert Total Amount in cart with Total Amount in invoice.");
                Assert.assertEquals(expectedTotalAmount, actualTotalAmount);
                System.out.println("Total Amount is equal to the total Amount in invoice.");
            } else {
                System.out.println("Not enough numbers extracted to perform assertions.");
            }
        } catch (TesseractException e) {
            e.printStackTrace();
            System.out.println("Error while extracting text from image: " + e.getMessage());
        }

        Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        Thread.sleep(2000);
        additionForProductScreen.navigateBack();
        Allure.step("verify to Navigate to Invoice to compare total price in the screen with total price in the cart.");
        additionForProductScreen.goToInvoices();
        Thread.sleep(3000);
        //Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        String TotalPrice = additionForProductScreen.getTotalInvoicePrice();
        System.out.println("إجمالى الفاتورة الموجود فى صفحة الفواتير: " + TotalPrice);
        System.out.println("*****************************************************");
        // Remove non-numeric characters from TotalPrice
        String cleanedTotalPrice = TotalPrice.replaceAll("[^0-9.]", "");
        // Compare TotalPrice with the last extracted number
        if (!numbers.isEmpty()) {
            double totalPriceValue = Double.parseDouble(cleanedTotalPrice);
            double lastNumberValue = Double.parseDouble(numbers.get(numbers.size() - 1));
            Assert.assertEquals(totalPriceValue, lastNumberValue); // Specify a delta value for double comparison
            System.out.println("TotalPrice is equal to the total Price number in additionForProductScreen screen.");
            Allure.addAttachment("Test Output", "text/plain", "Assertion Result is: " + "TotalPrice is equal to the total Price number in additionForProductScreen screen.");
        } else {
            System.out.println("No number extracted from details to compare with TotalPrice.");
        }

        System.out.println("********************************************");
        additionForProductScreen.clickOnReprint();
        Thread.sleep(3000);
        Allure.addAttachment("ScreenShot for Reprint Invoice", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        // Capture the screenshot and save it to a file
        File screenshotReprint = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File screenshotFileReprint = new File("invoice_screenshot.png");
        FileUtils.copyFile(screenshotReprint, screenshotFileReprint);
        // Use Tesseract to extract text from the image
        ITesseract tesseractReprint = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("Arabic"); // Set the language to Arabic
        String extractedTextReprint;
        try {
            extractedTextReprint = tesseract.doOCR(screenshotFileReprint);
            System.out.println("البيانات المستخرجه من الفاتورة كلها");
            System.out.println("----------------------------------");
            System.out.println(extractedTextReprint);
            Allure.addAttachment("Extracted Invoice Text", "text/plain", extractedTextReprint);
            // Filter the extracted text
            String[] lines = extractedTextReprint.split("\n");
            StringBuilder filteredText = new StringBuilder();
            // Patterns to match the lines of interest
            String[] keywords = {
                    "الخصم",
                    "إجمالى المبلغ المستحق"
            };
            for (String line : lines) {
                for (String keyword : keywords) {
                    if (line.contains(keyword)) {
                        // Remove intermediate numbers
                        line = line.replaceAll("\\s+\\d+/\\d+\\s+", " ");
                        line = line.replaceAll("\\s+\\d+\\s+", " ");
                        filteredText.append(line).append("\n");
                        break;
                    }
                }
            }
            // Remove the unwanted line if it appears in the filtered text
            String result = filteredText.toString().replaceAll("كاش \\d+\\s*", "").trim();
            System.out.println("----------------------------------");
            System.out.println("البيانات المطلوبة من الفاتورة");
            System.out.println("----------------------------------");
            System.out.println(result);
            Allure.addAttachment("Filtered Invoice Text", "text/plain", result);
            // Extract values by key
            String totalAmountReprint = extractValueByKey(result, "إجمالى المبلغ المستحق");
            Assert.assertEquals(totalAmount, totalAmountReprint);
            System.out.println("total Amount is: " + totalAmount);
            System.out.println("total Amount Reprint is: " + totalAmountReprint);
            // Accessing the first, second, and third numbers
            additionForProductScreen.navigateBack();
            additionForProductScreen.navigateBack();
            additionForProductScreen.navigateBack();
            additionForProductScreen.hideKeyboard();
            System.out.println("Invoice Small Tax Added Successfully");
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(priority = 2)
    public void addTwoProductsWithSameAddition() throws InterruptedException, IOException {
        additionForProductScreen = new AdditionForProductScreen();
        additionForProductScreen.addProduct();
        additionForProductScreen.addProduct2();
        additionForProductScreen.hideKeyboard();
        additionForProductScreen.goToCart();
        Thread.sleep(2000);
        Allure.addAttachment("Screenshot for Cart", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        additionForProductScreen.deleteProduct();
        Thread.sleep(2000);
        additionForProductScreen.clickOnNextBasket();
        Thread.sleep(3000);
        Allure.step("verify to get data like price and Tax an total price of the invoice.");
        String details = additionForProductScreen.getDetailsOfInvoice();

        System.out.println("بيانات الفاتورة الموجودة فى السلة");
        System.out.println("----------------------------------");
        // Define the regular expression pattern to match text and numbers
        Pattern pattern = Pattern.compile("([\\p{InArabic}\\s]+)|(SR \\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(details);
        // Skip initial texts if needed
        boolean skipInitialTexts = true;
        String text = " " + "إجمالى الضريبة";
        String lastText = "";
        List<String> numbers = new ArrayList<>();
        // Iterate through the matches and print the text and number
        while (matcher.find()) {
            String group = matcher.group();
            // Skip initial texts if needed
            if (skipInitialTexts) {
                if (group.contains("إجمالى الضريبة")) {
                    skipInitialTexts = false;
                }
                continue;
            }
            if (group.contains("SR")) {
                String number = group.substring(group.indexOf("SR") + 3); // Extract the number after "SR"
                numbers.add(number);
                lastText = text;
                System.out.println(text + number);
                Allure.addAttachment("Test Output", "text/plain", "Invoice Details Are: " + text + number);
            } else {
                text = group;
            }
        }
        // Print the last text and number
        if (!lastText.isEmpty()) {
            System.out.println(lastText + numbers.get(numbers.size() - 1));
            Allure.addAttachment("Test Output", "text/plain", "Invoice Details Are: " + lastText + numbers.get(numbers.size() - 1));
        }
        System.out.println("----------------------------------");
        Allure.step("verify to do payment and print the invoice.");
        additionForProductScreen.clickOnCashButton();
        Thread.sleep(4000);
        // Capture the screenshot and save it to a file
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File screenshotFile = new File("invoice_screenshot.png");
        FileUtils.copyFile(screenshot, screenshotFile);
        // Use Tesseract to extract text from the image
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("Arabic"); // Set the language to Arabic
        String extractedText;
        try {
            extractedText = tesseract.doOCR(screenshotFile);
            System.out.println("البيانات المستخرجه من الفاتورة كلها");
            System.out.println("----------------------------------");
            System.out.println(extractedText);
            Allure.addAttachment("Extracted Invoice Text", "text/plain", extractedText);
            // Filter the extracted text
            String[] lines = extractedText.split("\n");
            StringBuilder filteredText = new StringBuilder();
            // Patterns to match the lines of interest
            String[] keywords = {
                    "الخصم",
                    "إجمالى المبلغ المستحق"
            };
            for (String line : lines) {
                for (String keyword : keywords) {
                    if (line.contains(keyword)) {
                        // Remove intermediate numbers
                        line = line.replaceAll("\\s+\\d+/\\d+\\s+", " ");
                        line = line.replaceAll("\\s+\\d+\\s+", " ");
                        filteredText.append(line).append("\n");
                        break;
                    }
                }
            }
            // Remove the unwanted line if it appears in the filtered text
            String result = filteredText.toString().replaceAll("كاش \\d+\\s*", "").trim();
            System.out.println("----------------------------------");
            System.out.println("البيانات المطلوبة من الفاتورة");
            System.out.println("----------------------------------");
            System.out.println(result);
            Allure.addAttachment("Filtered Invoice Text", "text/plain", result);
            // Extract values by key
            totalAmount = extractValueByKey(result, "إجمالى المبلغ المستحق");
            // Accessing the first, second, and third numbers
            System.out.println("البيانات المطلوبة من السلة");
            System.out.println("----------------------------------");
            if (numbers.size() >= 3) {
                String firstNumber = numbers.get(0);
                String secondNumber = numbers.get(1);
                String thirdNumber = numbers.get(2);
                System.out.println("إجمالى الضريبة(السلة) : " + firstNumber);
                System.out.println("الخصم(السلة) : " + secondNumber);
                System.out.println("المجموع(السلة) : " + thirdNumber);
                // Convert strings to BigDecimal for comparison
                BigDecimal expectedTotalAmount = new BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP);
                BigDecimal actualTotalAmount = new BigDecimal(thirdNumber).setScale(2, RoundingMode.HALF_UP);
                // Assertions
                Allure.step("Assert Total Amount in cart with Total Amount in invoice.");
                Assert.assertEquals(expectedTotalAmount, actualTotalAmount);
                System.out.println("Total Amount is equal to the total Amount in invoice.");
            } else {
                System.out.println("Not enough numbers extracted to perform assertions.");
            }
        } catch (TesseractException e) {
            e.printStackTrace();
            System.out.println("Error while extracting text from image: " + e.getMessage());
        }

        Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        Thread.sleep(2000);
        additionForProductScreen.navigateBack();
        Allure.step("verify to Navigate to Invoice to compare total price in the screen with total price in the cart.");
        additionForProductScreen.goToInvoices();
        Thread.sleep(3000);
        //Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        String TotalPrice = additionForProductScreen.getTotalInvoicePrice();
        System.out.println("إجمالى الفاتورة الموجود فى صفحة الفواتير: " + TotalPrice);
        System.out.println("*****************************************************");
        // Remove non-numeric characters from TotalPrice
        String cleanedTotalPrice = TotalPrice.replaceAll("[^0-9.]", "");
        // Compare TotalPrice with the last extracted number
        if (!numbers.isEmpty()) {
            double totalPriceValue = Double.parseDouble(cleanedTotalPrice);
            double lastNumberValue = Double.parseDouble(numbers.get(numbers.size() - 1));
            Assert.assertEquals(totalPriceValue, lastNumberValue); // Specify a delta value for double comparison
            System.out.println("TotalPrice is equal to the total Price number in additionForProductScreen screen.");
            Allure.addAttachment("Test Output", "text/plain", "Assertion Result is: " + "TotalPrice is equal to the total Price number in additionForProductScreen screen.");
        } else {
            System.out.println("No number extracted from details to compare with TotalPrice.");
        }

        System.out.println("********************************************");
        additionForProductScreen.clickOnReprint();
        Thread.sleep(3000);
        Allure.addAttachment("ScreenShot for Reprint Invoice", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        // Capture the screenshot and save it to a file
        File screenshotReprint = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File screenshotFileReprint = new File("invoice_screenshot.png");
        FileUtils.copyFile(screenshotReprint, screenshotFileReprint);
        // Use Tesseract to extract text from the image
        ITesseract tesseractReprint = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("Arabic"); // Set the language to Arabic
        String extractedTextReprint;
        try {
            extractedTextReprint = tesseract.doOCR(screenshotFileReprint);
            System.out.println("البيانات المستخرجه من الفاتورة كلها");
            System.out.println("----------------------------------");
            System.out.println(extractedTextReprint);
            Allure.addAttachment("Extracted Invoice Text", "text/plain", extractedTextReprint);
            // Filter the extracted text
            String[] lines = extractedTextReprint.split("\n");
            StringBuilder filteredText = new StringBuilder();
            // Patterns to match the lines of interest
            String[] keywords = {
                    "الخصم",
                    "إجمالى المبلغ المستحق"
            };
            for (String line : lines) {
                for (String keyword : keywords) {
                    if (line.contains(keyword)) {
                        // Remove intermediate numbers
                        line = line.replaceAll("\\s+\\d+/\\d+\\s+", " ");
                        line = line.replaceAll("\\s+\\d+\\s+", " ");
                        filteredText.append(line).append("\n");
                        break;
                    }
                }
            }
            // Remove the unwanted line if it appears in the filtered text
            String result = filteredText.toString().replaceAll("كاش \\d+\\s*", "").trim();
            System.out.println("----------------------------------");
            System.out.println("البيانات المطلوبة من الفاتورة");
            System.out.println("----------------------------------");
            System.out.println(result);
            Allure.addAttachment("Filtered Invoice Text", "text/plain", result);
            // Extract values by key
            String totalAmountReprint = extractValueByKey(result, "إجمالى المبلغ المستحق");
            Assert.assertEquals(totalAmount, totalAmountReprint);
            System.out.println("total Amount is: " + totalAmount);
            System.out.println("total Amount Reprint is: " + totalAmountReprint);
            // Accessing the first, second, and third numbers
            additionForProductScreen.navigateBack();
            additionForProductScreen.navigateBack();
            additionForProductScreen.navigateBack();
            additionForProductScreen.hideKeyboard();
            System.out.println("Invoice Small Tax Added Successfully");
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }
    }
}