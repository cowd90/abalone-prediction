package com.cowd.machinelearning.controller;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.model.PMMLUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prediction")
@Slf4j
public class PredictionController {

    private Evaluator evaluator;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("stacking_model.pmml")) {
            if (is == null) {
                throw new IllegalArgumentException("File not found!");
            }

            PMML pmml = PMMLUtil.unmarshal(is);
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            this.evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);

        } catch (Exception e) {
            log.error("Error during PMML file unmarshalling", e);
            throw new RuntimeException("Error unmarshalling PMML file", e);
        }
    }


    @PostMapping
    public Map<String, Object> predict(@RequestBody Map<String, Object> input) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        String gender = input.get("Sex").toString();
        double sexMValue = 0.0;
        double sexIValue = 0.0;

        if (gender.equals("Male")) {
            sexMValue = 1.0;
        } else if (gender.equals("Infant")) {
            sexIValue = 1.0;
        }

        for (InputField inputField : this.evaluator.getInputFields()) {
            FieldName inputFieldName = inputField.getName();
            Object rawValue = input.get(inputFieldName.getValue());

            // Xử lý các biến chỉ báo giới tính
            if (inputFieldName.getValue().equals("Sex_M")) {
                arguments.put(inputFieldName, inputField.prepare(sexMValue));
            } else if (inputFieldName.getValue().equals("Sex_I")) {
                arguments.put(inputFieldName, inputField.prepare(sexIValue));
            } else {
                FieldValue inputFieldValue = inputField.prepare(rawValue);
                arguments.put(inputFieldName, inputFieldValue);
            }
        }

        Map<FieldName, ?> results = this.evaluator.evaluate(arguments);
        Map<String, Object> resultRecord = new LinkedHashMap<>();

        for (Map.Entry<FieldName, ?> entry : results.entrySet()) {
            resultRecord.put(entry.getKey().getValue(), entry.getValue());
        }
        log.info("RESULT:: " + resultRecord);
        return resultRecord;
    }
}
