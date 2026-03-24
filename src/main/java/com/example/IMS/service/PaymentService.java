package com.example.IMS.service;

import com.example.IMS.dto.PaymentRequest;
import com.example.IMS.dto.PaymentResponse;
import com.example.IMS.dto.PaymentVerificationRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${razorpay.company.name}")
    private String companyName;

    /**
     * Create a new payment order
     */
    public PaymentResponse createOrder(PaymentRequest paymentRequest) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create order request
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (paymentRequest.getAmount() * 100)); // Amount in paise
            orderRequest.put("currency", paymentRequest.getCurrency() != null ? paymentRequest.getCurrency() : currency);
            orderRequest.put("receipt", paymentRequest.getReceipt() != null ? paymentRequest.getReceipt() : generateReceipt());

            // Create order
            Order order = razorpayClient.orders.create(orderRequest);

            // Build response
            PaymentResponse response = new PaymentResponse();
            response.setOrderId(order.get("id"));
            response.setAmount(paymentRequest.getAmount());
            response.setCurrency(order.get("currency"));
            response.setReceipt(order.get("receipt"));
            response.setStatus(order.get("status"));
            
            // Handle created_at - convert to Long if it's an Integer
            Object createdAtObj = order.get("created_at");
            if (createdAtObj instanceof Integer) {
                response.setCreatedAt(((Integer) createdAtObj).longValue());
            } else if (createdAtObj instanceof Long) {
                response.setCreatedAt((Long) createdAtObj);
            } else {
                response.setCreatedAt(System.currentTimeMillis() / 1000); // fallback to current timestamp
            }
            
            response.setRazorpayKeyId(razorpayKeyId);
            response.setCustomerName(paymentRequest.getCustomerName());
            response.setCustomerEmail(paymentRequest.getCustomerEmail());
            response.setCustomerPhone(paymentRequest.getCustomerPhone());
            response.setDescription(paymentRequest.getDescription() != null ? 
                paymentRequest.getDescription() : "Payment to " + companyName);
            response.setMessage("Order created successfully");

            System.out.println("Payment order created: " + response.getOrderId());
            return response;

        } catch (RazorpayException e) {
            System.err.println("Razorpay Error creating payment order: " + e.getMessage());
            e.printStackTrace();
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Failed to create payment order: " + e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            System.err.println("Unexpected error creating payment order: " + e.getMessage());
            e.printStackTrace();
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Failed to create payment order: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(PaymentVerificationRequest verificationRequest) {
        try {
            String orderId = verificationRequest.getRazorpayOrderId();
            String paymentId = verificationRequest.getRazorpayPaymentId();
            String signature = verificationRequest.getRazorpaySignature();

            // Create signature verification string
            String payload = orderId + "|" + paymentId;

            // Generate expected signature
            String expectedSignature = generateSignature(payload, razorpayKeySecret);

            boolean isValid = expectedSignature.equals(signature);
            
            if (isValid) {
                System.out.println("Payment signature verified successfully");
            } else {
                System.err.println("Payment signature verification failed");
            }

            return isValid;

        } catch (Exception e) {
            System.err.println("Error verifying payment signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    /**
     * Generate a unique receipt number
     */
    private String generateReceipt() {
        return "RCPT_" + System.currentTimeMillis();
    }

    /**
     * Get payment details
     */
    public String getPaymentDetails(String paymentId) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            return razorpayClient.payments.fetch(paymentId).toString();
        } catch (RazorpayException e) {
            System.err.println("Error fetching payment details: " + e.getMessage());
            return null;
        }
    }

    /**
     * Refund a payment
     */
    public String refundPayment(String paymentId, double amount) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            JSONObject refundRequest = new JSONObject();
            if (amount > 0) {
                refundRequest.put("amount", (int) (amount * 100)); // Amount in paise
            }
            
            return razorpayClient.payments.refund(paymentId, refundRequest).toString();
        } catch (RazorpayException e) {
            System.err.println("Error processing refund: " + e.getMessage());
            return null;
        }
    }
}
