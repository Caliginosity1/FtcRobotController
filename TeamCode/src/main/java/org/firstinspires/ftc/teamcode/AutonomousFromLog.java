package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Autonomous(name="AutonomousFromLog", group="Linear Opmode")
public class AutonomousFromLog extends LinearOpMode {
    private DcMotor frontLeft, frontRight, backLeft, backRight, chainDrive, frontArm;
    private CRServo armRotator, dropper;

    private static final String CONTROL_DATA_FILE_PATH = "/storage/emulated/0/FIRST/ControlData.txt";

    @Override
    public void runOpMode() {
        initializeHardware();
        waitForStart();
        if (opModeIsActive()) {
            playBackControlData();
        }
    }

    private void initializeHardware() {
        frontLeft = hardwareMap.get(DcMotor.class, "front_left_motor");
        frontRight = hardwareMap.get(DcMotor.class, "front_right_motor");
        backLeft = hardwareMap.get(DcMotor.class, "back_left_motor");
        backRight = hardwareMap.get(DcMotor.class, "back_right_motor");
        chainDrive = hardwareMap.get(DcMotor.class, "chain_drive");
        frontArm = hardwareMap.get(DcMotor.class, "front_arm_motor");
        armRotator = hardwareMap.get(CRServo.class, "arm_rotator_servo");
        dropper = hardwareMap.get(CRServo.class, "drop_servo");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontArm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void playBackControlData() {
        File controlDataFile = new File(CONTROL_DATA_FILE_PATH);
        if (controlDataFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(controlDataFile))) {
                String line;
                while ((line = reader.readLine()) != null && opModeIsActive()) {
                    if (!line.isEmpty()) {
                        String[] tokens = line.split(",");
                        if (tokens.length >= 9) {
                            long timestamp = Long.parseLong(tokens[0]);
                            double frontLeftPower = Double.parseDouble(tokens[1]);
                            double frontRightPower = Double.parseDouble(tokens[2]);
                            double backLeftPower = Double.parseDouble(tokens[3]);
                            double backRightPower = Double.parseDouble(tokens[4]);
                            double armPower = Double.parseDouble(tokens[5]);
                            double chainPower = Double.parseDouble(tokens[6]);
                            double armRotatorPower = Double.parseDouble(tokens[7]);
                            double dropperPower = Double.parseDouble(tokens[8]);

                            frontLeft.setPower(frontLeftPower);
                            frontRight.setPower(frontRightPower);
                            backLeft.setPower(backLeftPower);
                            backRight.setPower(backRightPower);
                            frontArm.setPower(armPower);
                            chainDrive.setPower(chainPower);
                            armRotator.setPower(armRotatorPower);
                            dropper.setPower(dropperPower);

                            // Wait for the recorded time interval before applying the next set of values
                            long waitTime = timestamp - System.currentTimeMillis();
                            if (waitTime > 0) {
                                sleep(waitTime);
                            }
                        } else {
                            telemetry.addData("Error", "Not enough data in line: " + line);
                            telemetry.update();
                        }
                    } else {
                        telemetry.addData("Error", "Empty line in file");
                        telemetry.update();
                    }
                }
            } catch (IOException e) {
                telemetry.addData("Error", "Failed to read control data: " + e.getMessage());
                telemetry.update();
            } catch (NumberFormatException e) {
                telemetry.addData("Error", "Invalid format in control data file: " + e.getMessage());
                telemetry.update();
            }
        } else {
            telemetry.addData("Error", "Control data file not found");
            telemetry.update();
        }
    }
}

