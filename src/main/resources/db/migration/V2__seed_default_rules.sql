INSERT INTO monitoring_rules (name, type, parameters, enabled) VALUES
                                                                   (
                                                                       'High latency alert',
                                                                       'HIGH_LATENCY',
                                                                       '{"thresholdMs": 12}',
                                                                       true
                                                                   ),
                                                                   (
                                                                       'Rate limit exceeded',
                                                                       'FAULTY_EVENT',
                                                                       '{"errorCode": 900803}',
                                                                       true
                                                                   ),
                                                                   (
                                                                       'Delete event alert',
                                                                       'DELETE_EVENT',
                                                                       '{}',
                                                                       true
                                                                   ),
                                                                   (
                                                                       'PizzaShack call threshold',
                                                                       'THRESHOLD',
                                                                       '{"apiName": "PizzaShackAPI", "windowMinutes": 5, "callCountLimit": 20}',
                                                                       true
                                                                   );