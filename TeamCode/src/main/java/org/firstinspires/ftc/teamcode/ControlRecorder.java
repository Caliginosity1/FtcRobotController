package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;

import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@TeleOp(name="ControlRecorder", group="Linear Opmode")
public class ControlRecorder extends LinearOpMode {
    private DcMotor frontLeft, frontRight, backLeft, backRight, chainDrive, frontArm;
    private CRServo armRotator, dropper;

    private int upperLimit = -45;
    private int lowerLimit = -130;
    private double armSpeed = 0.75;

    private List<ControlData> controlDataList = new ArrayList<>();

    private static final String CONTROL_DATA_FILE_NAME = "ControlData.txt";
    private static final String CONTROL_DATA_DIR = Environment.getExternalStorageDirectory() + "/FIRST";
    private static final String CONTROL_DATA_FILE_PATH = CONTROL_DATA_DIR + "/" + CONTROL_DATA_FILE_NAME;

    private static class ControlData {
        long timestamp;
        double frontLeftPower, frontRightPower, backLeftPower, backRightPower;
        double armPower, chainPower, armRotatorPower, dropperPower;

        ControlData(long timestamp, double fl, double fr, double bl, double br,
                    double armP, double chainP, double armRotatorP, double dropperP) {
            this.timestamp = timestamp;
            frontLeftPower = fl;
            frontRightPower = fr;
            backLeftPower = bl;
            backRightPower = br;
            armPower = armP;
            chainPower = chainP;
            armRotatorPower = armRotatorP;
            dropperPower = dropperP;
        }
    }

    @Override
    public void runOpMode() {
        initializeHardware();
        waitForStart();
        while (opModeIsActive()) {
            processInputs();
            recordInputs();
            if (gamepad1.y) {
                telemetry.clear();
                telemetry.addData("Status", "Saving...");
                telemetry.update();
                saveControlData();
                telemetry.clear();
                telemetry.addData("Status", "Control data saved!");
                telemetry.update();
            } else {
                telemetry.clear();
                telemetry.addData("Status", "Press 'Y' to save control data.");
                telemetry.update();
            }
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

    private void processInputs() {
        double drive = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_stick_x;
        double rotate = gamepad1.right_stick_x;
        double frontLeftPower = drive + strafe + rotate;
        double frontRightPower = drive - strafe - rotate;
        double backLeftPower = drive - strafe + rotate;
        double backRightPower = drive + strafe - rotate;

        double max = Math.max(Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                Math.max(Math.abs(backLeftPower), Math.abs(backRightPower)));
        if (max > 1.0) {
            frontLeftPower /= max;
            frontRightPower /= max;
            backLeftPower /= max;
            backRightPower /= max;
        }

        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);

        double armPower = 0;
        int armPosition = frontArm.getCurrentPosition();
        if (armPosition >= upperLimit && gamepad1.right_trigger > 0) {
            armPower = 0;
        } else if (armPosition <= lowerLimit && gamepad1.left_trigger > 0) {
            armPower = (gamepad1.right_trigger - gamepad1.left_trigger) * armSpeed * 0.25;
        } else {
            armPower = (gamepad1.right_trigger - gamepad1.left_trigger) * armSpeed;
        }
        frontArm.setPower(armPower);

        double chainPower = gamepad2.right_trigger - gamepad2.left_trigger;
        chainDrive.setPower(chainPower);

        double armRotatorPower = gamepad2.left_stick_y * 0.65;
        armRotator.setPower(armRotatorPower);

        if (gamepad2.a) {
            dropper.setPower(-1.0);
            sleep(400);
            dropper.setPower(0);
            sleep(250);
            dropper.setPower(1.0);
            sleep(400);
            dropper.setPower(0);
        }

        if (gamepad2.right_bumper) {
            dropper.setPower(0.75);
        } else if (gamepad2.left_bumper) {
            dropper.setPower(-0.75);
        } else {
            dropper.setPower(0);
        }
    }

    private void recordInputs() {
        long currentTime = System.currentTimeMillis();
        controlDataList.add(new ControlData(
                currentTime,
                frontLeft.getPower(), frontRight.getPower(), backLeft.getPower(), backRight.getPower(),
                frontArm.getPower(), chainDrive.getPower(), armRotator.getPower(), dropper.getPower()
        ));
    }

    private void saveControlData() {
        File file = new File(CONTROL_DATA_DIR, CONTROL_DATA_FILE_NAME);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        int dataSize = controlDataList.size();
        int progress = 0;
        int step = 0;
        int stepSize = dataSize / 100; // Step size for 1% progress, avoid division by zero if dataSize is less than 100

        try (FileWriter writer = new FileWriter(file, false)) {
            for (int i = 0; i < dataSize; i++) {
                ControlData data = controlDataList.get(i);
                writer.write(String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                        data.timestamp,
                        data.frontLeftPower, data.frontRightPower, data.backLeftPower, data.backRightPower,
                        data.armPower, data.chainPower, data.armRotatorPower, data.dropperPower));
                // Update progress bar every stepSize records
                if (i >= step * stepSize) {
                    progress = (i + 1) * 100 / dataSize;
                    telemetry.clear();
                    telemetry.addData("Saving", "Progress: %d%%", progress);
                    telemetry.update();
                    step++;
                }
            }
            // Ensure the progress bar is full when the save operation completes
            telemetry.clear();
            telemetry.addData("Saving", "Progress: %d%%", 100);
            telemetry.update();
        } catch (IOException e) {
            telemetry.clear();
            telemetry.addData("Error", "Failed to save recorded data: " + e.getMessage());
            telemetry.update();
        }
    }
}
