package GoooBigTestCasesForTablet;

import GoooBigBase.TestBase;
import GoooBigScreensForTablet.PriceOffer;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoooBig_PriceOffer extends TestBase {
    public String discountAmount = "500.00";
    PriceOffer priceOffer;
    private String TaxNumber = "";
    private String TotalPriceInvoice = "";

    @Test(priority = 1)
    public void priceOffer() throws InterruptedException, IOException {
        priceOffer = new PriceOffer();
        Thread.sleep(15000);
        Allure.step("verify to search for a product and add it in cart.");
        priceOffer.sendKeysToSearchProductTablet("باقة رمضان");
        priceOffer.hideKeyboard();
        priceOffer.clickOnProductFirstTablet();
        priceOffer.clearSearchProductTablet();
        priceOffer.hideKeyboard();
        priceOffer.sendKeysToSearchProductTablet("ايفون 12");
        priceOffer.hideKeyboard();
        priceOffer.clickOnProductSecondTablet();
        priceOffer.hideKeyboard();
        Allure.step("verify to Do discount over product.");
        priceOffer.clickOnEditProductTablet();
        priceOffer.sendKeysToDiscountTablet(discountAmount);
        priceOffer.hideKeyboard();
        String details = priceOffer.getDetailsOfInvoiceTablet();
        System.out.println(details);
        System.out.println("بيانات الفاتورة الموجودة فى السلة");
        System.out.println("----------------------------------");
        // Define the regular expression pattern to match text and numbers
        Pattern pattern = Pattern.compile("([\\p{InArabic}\\s]+)|(SR \\d+\\.\\d+|\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(details);

        // Define the set of keywords we are interested in
        Set<String> keywords = new HashSet<>(Arrays.asList("الضريبة", "الخصم", "الإجمالى"));

        // Iterate through the matches and print the text and number
        String text = "";
        List<String> numbers = new ArrayList<>();

        while (matcher.find()) {
            String group = matcher.group().trim();
            if (keywords.contains(group)) {
                text = group;
            } else if (group.matches("\\d+\\.\\d+")) {
                if (keywords.contains(text)) {
                    numbers.add(group);
                    System.out.println(text + ": " + group);
                    Allure.addAttachment("Test Output", "text/plain", "Invoice Details Are: " + text + ": " + group);
                }
            }
        }
        System.out.println("----------------------------------");
        // Compare TotalPrice with the last extracted number
        if (numbers.size() >= 3) {
            String extractedTax = numbers.get(0);
            String extractedDiscount = numbers.get(1);
            String extractedTotal = numbers.get(2);
            System.out.println("إجمالى الضريبة(السلة) : " + extractedTax);
            System.out.println("الخصم(السلة) : " + extractedDiscount);
            System.out.println("المجموع(السلة) : " + extractedTotal);
            try {
                double expectedDiscountNum = Double.parseDouble(discountAmount);
                double actualDiscountNum = Double.parseDouble(extractedDiscount);
                System.out.println("expectedTotalNum: " + expectedDiscountNum);
                System.out.println("actualTotalNum: " + actualDiscountNum);
                Assert.assertEquals(expectedDiscountNum, actualDiscountNum, "Discount values do not match.");
                System.out.println("Discount is equal to the Discount number in invoices screen.");
                Allure.addAttachment("Test Output", "text/plain", "Assertion Result: Discount is equal to the Discount number in invoices screen.");
            } catch (NumberFormatException e) {
                System.err.println("Error parsing numeric values: " + e.getMessage());
            }
        } else {
            System.out.println("No number extracted from details to compare with TotalPrice.");
        }
        priceOffer.clickOnNextScreen();
        Thread.sleep(2000);
        priceOffer.clickOnDeliveryMethod();
        priceOffer.clickOnPriceOfferButton();
        Thread.sleep(6000);
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
            String[] keywordsArray = {
                    "الخصم",
                    "مجموع ضريبة القيمة المضافة",
                    "إجمالى المبلغ المستحق",
                    "إجمالى المبلخغ المستحق"
            };

            List<String> extractedNumbers = new ArrayList<>();
            for (String line : lines) {
                for (String keyword : keywordsArray) {
                    if (line.contains(keyword)) {
                        // Extract numeric value from the line
                        String numericValue = line.replaceAll("[^0-9.]", "");
                        if (!numericValue.isEmpty()) {
                            extractedNumbers.add(numericValue);
                        }
                        filteredText.append(line).append("\n");
                        break;
                    }
                }
            }

            // Print filtered text
            System.out.println("البيانات المفلترة من الفاتورة:");
            System.out.println(filteredText);
            Allure.addAttachment("Filtered Invoice Text", "text/plain", filteredText.toString());

            // Initialize and extract TaxNumber
            if (extractedNumbers.size() > 1) {
                TaxNumber = extractedNumbers.get(1);
                TotalPriceInvoice = extractedNumbers.get(2);
            } else {
                TaxNumber = "0"; // Assign a default value if extraction fails
                TotalPriceInvoice = "0";
            }

        } catch (TesseractException e) {
            e.printStackTrace();
            return;
        }

        Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        Thread.sleep(2000);
        priceOffer.navigateBack();
        Allure.step("verify to Navigate to Invoice to compare total price in the screen with total price in the cart.");
        priceOffer.clickInvoicesScreen();
        Thread.sleep(2000);
        priceOffer.clickPriceOfferInvoices();
        Thread.sleep(2000);
        //Allure.addAttachment("Screenshot for result", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        String TotalPrice = priceOffer.getTotalInvoicePriceTablet();
        System.out.println("إجمالى الفاتورة الموجود فى صفحة الفواتير: " + TotalPrice);
        System.out.println("*****************************************************");
        // Remove non-numeric characters from TotalPrice
        String cleanedTotalPrice = TotalPrice.replaceAll("[^0-9.]", "");

        // Compare TotalPrice with the last extracted number
        if (numbers.size() >= 3) {
            String extractedTax = numbers.get(0);
            String extractedDiscount = numbers.get(1);
            String extractedTotal = numbers.get(2);
            System.out.println("إجمالى الضريبة(السلة) : " + extractedTax);
            System.out.println("الخصم(السلة) : " + extractedDiscount);
            System.out.println("المجموع(السلة) : " + extractedTotal);
            try {
                double expectedTotalNum = Double.parseDouble(TotalPriceInvoice);
                double actualTotalNum = Double.parseDouble(extractedTotal);
                System.out.println("expectedTotalNum: " + expectedTotalNum);
                System.out.println("actualTotalNum: " + actualTotalNum);
                Assert.assertEquals(expectedTotalNum, actualTotalNum, "Total values do not match.");
                System.out.println("TotalPrice is equal to the total price number in invoices screen.");

                double expectedTaxNumber = Double.parseDouble(TaxNumber);
                double actualTaxNumber = Double.parseDouble(extractedTax);
                Assert.assertEquals(expectedTaxNumber, actualTaxNumber, "Tax values do not match.");
                System.out.println("TotalPrice is equal to the tax number in invoices screen.");

                double totalInvoiceValue = Double.parseDouble(cleanedTotalPrice);
                double extractedTotalValue = Double.parseDouble(extractedTotal);
                Assert.assertEquals(totalInvoiceValue, extractedTotalValue, "Total invoice values do not match.");
                System.out.println("TotalPrice is equal to the total price number in invoices screen.");

                Allure.addAttachment("Test Output", "text/plain", "Assertion Result: TotalPrice is equal to the total price number in invoices screen.");
            } catch (NumberFormatException e) {
                System.err.println("Error parsing numeric values: " + e.getMessage());
            }
        } else {
            System.out.println("No number extracted from details to compare with TotalPrice.");
        }

        System.out.println("********************************************");
        priceOffer.navigateBack();
        priceOffer.clearSearchProductTablet();
        priceOffer.hideKeyboard();
    }
}
