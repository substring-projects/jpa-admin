package com.substringtechnologies.jpaadmin.metadata;

import com.substringtechnologies.jpaadmin.annotations.AdminWidget;
import com.substringtechnologies.jpaadmin.annotations.WidgetType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WidgetOptionsTest {
    @SuppressWarnings("unused")
    static class Misconfigured {
        @AdminWidget(type = WidgetType.SLIDER, min = 0) Integer sliderWithoutMax;
        @AdminWidget(min = 5, max = 1) Integer reversedBounds;
        @AdminWidget(step = -1) Integer negativeStep;
        @AdminWidget(min = 0) String textWithBounds;
        @AdminWidget(type = WidgetType.CODE_EDITOR, language = "cobol") String unknownLanguage;
        @AdminWidget(type = WidgetType.IMAGE_URL, previewSize = "64px; background:url(x)") String injectedSize;
    }

    private String failure(String fieldName, FieldType type, WidgetType widget) throws Exception {
        var meta = new FieldMetadata();
        meta.setFieldType(type);
        meta.setWidget(widget);
        var field = Misconfigured.class.getDeclaredField(fieldName);
        return assertThrows(IllegalStateException.class, () -> new JpaMetamodelScanner(null).applyWidgetOptions(field, meta)).getMessage();
    }

    @Test
    void misconfiguredWidgetsFailStartupWithTheFieldName() throws Exception {
        assertTrue(failure("sliderWithoutMax", FieldType.NUMBER, WidgetType.SLIDER).contains("Misconfigured.sliderWithoutMax"));
        assertTrue(failure("reversedBounds", FieldType.NUMBER, WidgetType.AUTO).contains("min must not exceed max"));
        assertTrue(failure("negativeStep", FieldType.NUMBER, WidgetType.AUTO).contains("step"));
        assertTrue(failure("textWithBounds", FieldType.STRING, WidgetType.AUTO).contains("numeric"));
        assertTrue(failure("unknownLanguage", FieldType.STRING, WidgetType.CODE_EDITOR).contains("cobol"));
        assertTrue(failure("injectedSize", FieldType.STRING, WidgetType.IMAGE_URL).contains("previewSize"));
    }
}
