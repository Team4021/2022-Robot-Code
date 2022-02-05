// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

//starter imports
package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

//our imports
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.drive.DifferentialDrive; //change to whatever our drive is
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */

public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");
  NetworkTableEntry tv = table.getEntry("tv"); // 1 if have vision 0 if no vision

  //motors
    //drive motors
  WPI_TalonFX leftFront = new WPI_TalonFX(3);
  WPI_TalonFX leftBack = new WPI_TalonFX(1);
  WPI_TalonFX rightFront = new WPI_TalonFX(6); //set to the right values
  WPI_TalonFX rightBack = new WPI_TalonFX(5);
    //shooter motor
  WPI_TalonFX shooter = new WPI_TalonFX(8);
    //climber motors
  WPI_TalonFX masonUp = new WPI_TalonFX(9); //set to the right values
  WPI_TalonFX philDiag = new WPI_TalonFX(10);
  //intake and belt motors
  WPI_VictorSPX alexIntake = new WPI_VictorSPX(1);
  WPI_VictorSPX monkeyBelt1 = new WPI_VictorSPX(2); //set to the right values
  WPI_VictorSPX monkeyBelt2 = new WPI_VictorSPX(3);
  //motor control groups for belt
  MotorControllerGroup belt = new MotorControllerGroup(monkeyBelt1, monkeyBelt2);
  //motor control groups (for driving)
  MotorControllerGroup left = new MotorControllerGroup(leftFront, leftBack);
  MotorControllerGroup right = new MotorControllerGroup(rightFront, rightBack);
 
  //differential drive. What does this do again?
  DifferentialDrive moveItMoveIt = new DifferentialDrive(left, right);
  
  //Marty=X Melman=y 
    //a lot of these need more identifiable names
  double setpointX = 0; //where we want our limelight x to be
  double setpointY = 4;
  double martySpeed = 0;
  double martyAlign;
  double martyError = 0;
  double lmlx;
  double lmly;
  double melmanSpeed = 0;
  double melmanAlign;
  double melmanError;

  boolean alignedFirst;
  boolean alignedFinal;
  boolean distanced;

  Timer skipper = new Timer();

  //joystick - I petition we name this something more identifiable just in case something breaks
  Joystick mort = new Joystick(1);


  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);
  }

  /**
   * This function is called every robot packet, no matter the mode. Use this for items like
   * diagnostics that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    NetworkTableInstance.getDefault();

    //read values periodically
    double lmlx = tx.getDouble(0.0);
    double lmly = ty.getDouble(0.0);
    double area = ta.getDouble(0.0);

    //post to smart dashboard periodically
    SmartDashboard.putNumber("LimelightX", lmlx);
    SmartDashboard.putNumber("LimelightY", lmly);
    SmartDashboard.putNumber("LimelightArea", area);

    SmartDashboard.putBoolean("alignedFirst", alignedFirst);
    SmartDashboard.putBoolean("alignedFinal", alignedFinal);
    SmartDashboard.putBoolean("distanced", distanced);
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select between different
   * autonomous modes using the dashboard. The sendable chooser code works with the Java
   * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
   * uncomment the getString line to get the auto name from the text box below the Gyro
   *
   * <p>You can add additional auto modes by adding additional comparisons to the switch structure
   * below with additional strings. If using the SendableChooser make sure to add them to the
   * chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
    skipper.reset();
    skipper.start();
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      case kCustomAuto:
        // Put custom auto code here
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here
        if (skipper.get() < 10) {
          kowalski();
        } else {
          right.set(.3);
          left.set(-.3);
        }
        break;
    }
  }

  /** This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {}

  /** This function is called periodically during operator control. */
    //joystick imput = motion using arcade drive
  @Override
  public void teleopPeriodic() {
    double x = mort.getRawAxis(4); //figure out the right axis
    double y = mort.getRawAxis(1); //figure out the right axis
    double masonPower = mort.getRawAxis(3);
    double philPower = mort.getRawAxis(2);
    //drive controls
    moveItMoveIt.arcadeDrive(x, -y);
    //autoshoot
    if (mort.getRawButton(1)) {
      kowalski();
    }
    //shooter controls
    if (mort.getRawButton(4)) {
      shooter.set(.5); //set to the right values
    }
    //belt controls
    if (mort.getRawButton(2)) {
      belt.set(.5);
    } else if (mort.getRawButton(3)) {
      belt.set(-.5);
    } //set to the right values

    masonUp.set(masonPower);
    philDiag.set(philPower);

  }

  /** This function is called once when the robot is disabled. */
  @Override
  public void disabledInit() {}

  /** This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {}

  /** This function is called once when test mode is enabled. */
  @Override
  public void testInit() {}

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}
//Kowalski = aimbot
  public void kowalski() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    alignedFirst = false;
    alignedFinal = false;
    distanced = false;
    martyX();
    melmanY();
    double lmlx = tx.getDouble(0.0);
    double lmly = ty.getDouble(0.0);
  
    
    
    // find the first bounds to rotate into place
    if (tv == 0) {
      right.set(.3);
      left.set(.3);
      alignedFirst = false;
    } else if (lmlx > 10) {
      right.set(martyAlign);
      left.set(martyAlign);
      alignedFirst = false;
    } else if (lmlx < -10) {
      right.set(-martyAlign);
      left.set(-martyAlign);
      alignedFirst = false;
   } else if (lmlx < 10 && lmlx > -10 && tv == 1) {
     alignedFirst = true;
    }


    // after rotated into place, move closer
    if (alignedFirst == true && tv == 1) {
      if (lmly > 5) {
        right.set(melmanAlign);
        left.set(-melmanAlign);
        distanced = false;
      } else if (lmly < 3) {
        right.set(-melmanAlign);
        left.set(melmanAlign);
        distanced = false;
      } else if (lmly < 5 && lmly > 3 && tv == 1) {
        distanced = true;
      }
    }


    // after moving closer, rotate into a better position
    if (distanced == true) {
      if (lmlx > 1) {
        right.set(.05);
        left.set(.05);
        alignedFinal = false;
      } else if (lmlx < -1) {
        right.set(-.05);
        left.set(-.05);
        alignedFinal = false;
      } else if (lmlx < 1 && lmlx > -1 && tv == 1) {
        alignedFinal = true;
      }
      if (alignedFinal == true && alignedFirst == true && distanced == true) {
        shooter.set(.5);
        belt.set(.5);
      }
    }

  }

  /**********************************************************/
  public double martyX() {
    martySpeed = .03;
    martyError = setpointX - lmlx;
    if (Math.abs(martySpeed*martyError) < .15) {
      martyAlign = .15;
    } else {
      martyAlign = martySpeed*martyError;
    }
    return martyAlign;
  }

  /**********************************************************/
  public double melmanY() {
    melmanSpeed = .05;
    melmanError = setpointY - lmly;
    if (Math.abs(melmanSpeed*melmanError) < .15) {
      melmanAlign = .15;
    } else {
      melmanAlign = melmanSpeed*melmanError;
    }
    return melmanAlign;
  }

}
