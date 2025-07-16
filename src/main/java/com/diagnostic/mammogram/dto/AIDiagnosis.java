package com.diagnostic.mammogram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; // Needed for List<Double>

/**
 * DTO to capture the AI model's diagnosis response.
 * Matches the JSON structure returned by the Python Flask AI service.
 */
@Data // Lombok: Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Lombok: Generates no-argument constructor
@AllArgsConstructor // Lombok: Generates constructor with all fields
@Builder // Lombok: Generates builder pattern
public class AIDiagnosis {

    // This field will directly map to the 'prediction' key from the Flask app's JSON response
    private String prediction;

    // This field will directly map to the 'probabilities' array from the Flask app's JSON response
    private List<Double> probabilities;

    private Double confidenceScore;

    private String diagnosisSummary;

    // --- Derived/Computed Fields (not directly mapped from Flask JSON) ---
    // These methods provide a convenient way to get summary information
    // by combining the 'prediction' and 'probabilities' fields.

    /**
     * Computes a diagnosis summary string from the prediction and probabilities.
     * This is not directly mapped from the AI service's JSON but derived here.
     * @return A formatted string summarizing the prediction and confidence.
     */
    public String getDiagnosisSummary() {
        if (prediction != null && probabilities != null && !probabilities.isEmpty()) {
            // Assuming probabilities list is ordered [NORMAL_PROB, ABNORMAL_PROB]
            // We need to find the probability corresponding to the 'prediction' string.
            // A more robust way would be to pass class names or have a fixed order.
            // For a 2-class (NORMAL, ABNORMAL) scenario:
            Double predictedProbability = null;
            if (prediction.equalsIgnoreCase("NORMAL") && probabilities.size() > 0) {
                predictedProbability = probabilities.get(0);
            } else if (prediction.equalsIgnoreCase("ABNORMAL") && probabilities.size() > 1) {
                predictedProbability = probabilities.get(1);
            }

            if (predictedProbability != null) {
                return String.format("Predicted: %s (Confidence: %.2f%%)", prediction, predictedProbability * 100);
            }
        }
        return "AI Diagnosis N/A"; // Default if data is incomplete
    }

    /**
     * Computes the confidence score for the predicted class.
     * This is not directly mapped from the AI service's JSON but derived here.
     * @return The confidence score (probability) of the predicted class, or null if not available.
     */
    public Double getConfidenceScore() {
        if (prediction != null && probabilities != null && !probabilities.isEmpty()) {
            if (prediction.equalsIgnoreCase("NORMAL") && probabilities.size() > 0) {
                return probabilities.get(0);
            } else if (prediction.equalsIgnoreCase("ABNORMAL") && probabilities.size() > 1) {
                return probabilities.get(1);
            }
        }
        return null; // Or 0.0, depending on how you want to represent missing confidence
    }

    // The fields 'detailedFindings' and 'recommendation' are NOT provided by your current Flask app.
    // If you need these, you MUST modify your Python Flask app to include them in its JSON response.
    // For now, they would always be null if you included them here without Flask providing them.
    // private String detailedFindings;
    // private String recommendation;
}
