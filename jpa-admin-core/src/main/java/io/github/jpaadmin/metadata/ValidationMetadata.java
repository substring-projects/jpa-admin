package io.github.jpaadmin.metadata;

public class ValidationMetadata {
    private boolean required;
    private Double min;
    private Double max;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private boolean email;

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Double getMin() { return min; }
    public void setMin(Double min) { this.min = min; }

    public Double getMax() { return max; }
    public void setMax(Double max) { this.max = max; }

    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public boolean isEmail() { return email; }
    public void setEmail(boolean email) { this.email = email; }
}
