package com.codingame.runner;

import com.codingame.gameengine.runner.SoloGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class CommandLineRunner {
  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("h", false, "Display help information")
        .addOption("bot", true, "Required path to the bot executable to run")
        .addOption("all", false, "Run the bot on all test cases")
        .addOption("t", true, "Specify the test case number to use")
        .addOption("o", true, "Path to the JSON file to write to with the results")
        .addOption("data", false, "Retrieve data sent by the bot to display them in the report")
        .addOption("stderr", false, "Display all data sent to stderr by the bot")
      ;

      CommandLine command = new DefaultParser().parse(options, args);
      if (command.hasOption("h") || !command.hasOption("bot") || (!command.hasOption("all") && !command.hasOption("t"))) {
        new HelpFormatter().printHelp(
          "-bot path_to_bot_executable [-all | -t test_case_number] [-o output_json_file] [-data]",
          options);
        System.exit(0);
      }

      boolean testAll = command.hasOption("all");
      boolean extractData = command.hasOption("data");
      boolean showStderr = command.hasOption("stderr");

      List<Integer> testCaseNumbers = new ArrayList<Integer>();
      if (testAll) {
        for (int i = 1; i < 25; ++i) {
          testCaseNumbers.add(i);
        }
      }
      else {
        testCaseNumbers.add(Integer.parseInt(command.getOptionValue("t")));
      }

      float totalScore = 0;
      for (int  testCaseNumber : testCaseNumbers) {
        SoloGameRunner runner = new SoloGameRunner();
        runner.setAgent(command.getOptionValue("bot"));
        runner.setTestCase(String.format("test%d.json", testCaseNumber));
        GameResult result = runner.simulate();

        if (testAll && testCaseNumber == 13) {
          System.out.println(String.format("TOTAL SCORE = %07.0f\n", totalScore));
          totalScore = 0.f;
        }
        System.out.print(String.format("TEST CASE %02d: ", testCaseNumber));
        
        int turnCount = result.summaries.size();
        Gson gson = new Gson();
        float score = Float.parseFloat(gson.fromJson(result.metadata, JsonObject.class).get("points").getAsString());
        if (score > 0) {
          System.out.println(String.format("%07.0f", score));
          for (int i = 0; i < turnCount - 1; ++i) {
            if (result.summaries.get(i) != null && !result.summaries.get(i).isEmpty()) {
              System.out.println(String.format("   Error[turn=%d] %s", i, result.summaries.get(i)));
            }
          }
          totalScore += score;
        }
        else {
          System.out.println();
          for (int i = 0; i < turnCount; ++i) {
            if (result.summaries.get(i) != null && !result.summaries.get(i).isEmpty()) {
              System.out.println(String.format("   Error[turn=%d] %s", i, result.summaries.get(i)));
            }
          }
        }

        if (extractData) {
          List<String> botOutput = result.errors.get("0");
          int outputTurnCount = botOutput.size();
          for (int i = 0; i < outputTurnCount; ++i) {
            String thisTurnOutput = botOutput.get(i);
            if (thisTurnOutput == null || thisTurnOutput.isEmpty()) continue;
            for (String line : thisTurnOutput.split("\\r?\\n")) {
              if (line.startsWith("[DATA] ")) {
                line = line.substring(7);
                if (!line.isEmpty()) {
                  System.out.print(String.format("    Data[turn=%d] %s\n", i, line));
                }
              }
            }
          }
        } 
        if (showStderr) {
          List<String> botOutput = result.errors.get("0");
          int outputTurnCount = botOutput.size();
          for (int i = 0; i < outputTurnCount; ++i) {
            String thisTurnOutput = botOutput.get(i);
            if (thisTurnOutput == null || thisTurnOutput.isEmpty()) continue;
            for (String line : thisTurnOutput.split("\\r?\\n")) {
              if (!line.isEmpty()) {
                System.out.print(String.format("    Output[turn=%d] %s\n", i, line));
              }
            }
          }
        }                
      }

      System.out.println(String.format("TOTAL SCORE = %07.0f", totalScore));
    }
    catch (Exception e) {
      System.err.println(e);
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }
}