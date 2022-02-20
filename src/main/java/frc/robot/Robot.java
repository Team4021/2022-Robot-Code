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
import edu.wpi.first.wpilibj.drive.DifferentialDrive; 
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cameraserver.CameraServer;

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
  WPI_TalonFX leftFront = new WPI_TalonFX(4);
  WPI_TalonFX leftBack = new WPI_TalonFX(5);
  WPI_TalonFX rightFront = new WPI_TalonFX(3); //set to the right values
  WPI_TalonFX rightBack = new WPI_TalonFX(6);
    //shooter motor
  WPI_TalonFX shooter = new WPI_TalonFX(0);
    //climber motors
  WPI_TalonFX masonVert = new WPI_TalonFX(1); //set to the right values
  WPI_TalonFX philDiag = new WPI_TalonFX(10);
  //intake and belt motors
  WPI_TalonFX alexIntake = new WPI_TalonFX(2);
  //motor control group (belt)
  MotorControllerGroup left = new MotorControllerGroup(leftFront, leftBack);
  MotorControllerGroup right = new MotorControllerGroup(rightFront, rightBack);
 
  //differential drive. What does this do again?
  DifferentialDrive moveItMoveIt = new DifferentialDrive(left, right);
  
  //Marty=X Melman=y 
    //a lot of these need more identifiable names
  double setpointX = -2.13; //where we want our limelight x to be
  double setpointY = 1.9; //where we want our limelight y to be
  //nice
  double martySpeed = 0;
  double martyAlign;
  double martyError = 0; //how far our limelight is from its target (X)
  double lmlx; //where limelight x actually is
  double lmly; //where limelight y actually is
  double melmanSpeed = 0;
  double melmanAlign;
  double melmanError; //how far limelight is from its target (Y)

  //allign X once, allign Y, then reallign X
  boolean alignedFirst;
  boolean alignedFinal;
  boolean distanced;
  //autonomous timer to make sure we move for extra points
  Timer skipper = new Timer();
  Timer delayShot = new Timer();
  boolean autoOne = true;
  boolean intakeToggle;

  //camera
  UsbCamera cam0;
  double inpTest;
  //joystick - I petition we name this something more identifiable just in case something breaks
  Joystick mort = new Joystick(1);
    //Buttons
      //A=1   Limelight
      //B=2   Shooter
      //X=3   Reverse Shooter/Intake
      //Y=4   Intake
      //Left Bumper=5   Diagonal Climb Up
      //Right Bumper=6    Vertical Climb Up
      //Back (select)=7   Nothing
      //Start=8   Nothing
      //left Joystick Button=9
      //Right Joystick Button=10

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);
    cam0 = CameraServer.startAutomaticCapture(0);
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
    SmartDashboard.getNumber("Input Number", inpTest);
    if (inpTest == 420){
      SmartDashboard.putBoolean("Input Test", true);
    } else {
      SmartDashboard.putBoolean("Input Test", false);
    }
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
        if (skipper.get() < 2.5) {
          right.set(-.2);
          left.set(.2);
          shooter.set(.5);
        } else {
          right.set(0);
          left.set(0);
          shooter.set(.5);
          alexIntake.set(.3);
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
    double x = mort.getRawAxis(4)/1.5; 
    double y = mort.getRawAxis(1)/1.5; 
    double masonPower = mort.getRawAxis(3);
    double philPower = mort.getRawAxis(2);
    //drive controls
    moveItMoveIt.arcadeDrive(x, -y);
    //autoshoot
    if (mort.getRawButton(1))/*A*/ {
      kowalski();
    }

    //belt & shooter controls
    if (mort.getRawButton(2))/*B*/ {
      shooter.set(.55);
    } else if (mort.getRawButton(3))/*X*/ {
      alexIntake.set(-.3);
      shooter.set(-.3);
    } else if (mort.getRawButton(1))/*A*/ {
      kowalski();
    } else {
      alexIntake.set(0);
      shooter.set(0);
    }

    if (mort.getRawButtonPressed(4))  {
      intakeToggle = !intakeToggle; }
    if (intakeToggle == true) {
      alexIntake.set(.4);
      
    }
    else {
      alexIntake.set(0);
    }
      masonVert.set(masonPower);
      philDiag.set(philPower);
    

    if (mort.getRawButton(5))/*LB*/ {
      philDiag.set(-.25);
    } else if (mort.getRawButton(6))/*RB*/ {
      masonVert.set(-.25);
    }

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
    } else if (lmlx > 0) {
      right.set(martyAlign);
      left.set(martyAlign);
      alignedFirst = false;
    } else if (lmlx < -15) {
      right.set(-martyAlign);
      left.set(-martyAlign);
      alignedFirst = false;
   } else if (lmlx < 0 && lmlx > -15 && tv == 1) {
     alignedFirst = true;
    }


    // after rotated into place, move closer
    if (alignedFirst == true && tv == 1) {
      if (lmly > 2.4) {
        right.set(-melmanAlign);
        left.set(melmanAlign);
        distanced = false;
      } else if (lmly < 1.4) {
        right.set(melmanAlign);
        left.set(-melmanAlign);
        distanced = false;
      } else if (lmly < 2.4 && lmly > 1.4 && tv == 1) {
        distanced = true;
      }
    }


    // after moving closer, rotate into a better position
    if (distanced == true) {
      if (lmlx > -1.13) {
        right.set(.1);
        left.set(.1);
        alignedFinal = false;
      } else if (lmlx < -3.13) {
        right.set(-.1);
        left.set(-.1);
        alignedFinal = false;
      } else if (lmlx < -1.13 && lmlx > -3.13 && tv == 1) {
        alignedFinal = true;
      }
      if (alignedFinal == true) {
        shooter.set(.65);
        alexIntake.set(.25);
      }

    }

  }

  /**********************************************************/
  public double martyX() {
    martySpeed = .01;
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
    melmanSpeed = .02;
    melmanError = setpointY - lmly;
    if (Math.abs(melmanSpeed*melmanError) < .15) {
      melmanAlign = .15;
    } else {
      melmanAlign = melmanSpeed*melmanError;
    }
    return melmanAlign;
  }

}
