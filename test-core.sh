#!/bin/sh
set -eu
cd "$(dirname "$0")"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/app/m30/companion/ControllerConnectionRequest.java
javac -d "$out" app/src/main/java/app/m30/companion/ControllerAutoFollow.java tests/ControllerAutoFollowTest.java app/src/main/java/app/m30/companion/BluetoothDeviceLabel.java tests/BluetoothDeviceLabelTest.java app/src/main/java/app/m30/companion/BluetoothSlotLinks.java tests/BluetoothSlotLinksTest.java app/src/main/java/app/m30/companion/BluetoothDeviceList.java tests/BluetoothDeviceListTest.java app/src/main/java/app/m30/companion/BluetoothGamepadFilter.java app/src/main/java/app/m30/companion/BluetoothPairingState.java tests/BluetoothSearchTest.java app/src/main/java/app/m30/companion/RemoteNavigation.java tests/RemoteNavigationTest.java app/src/main/java/app/m30/companion/DpadDetector.java tests/DpadDetectorTest.java app/src/main/java/app/m30/companion/ControllerTelemetry.java tests/ControllerTelemetryTest.java app/src/main/java/app/m30/companion/InputTestState.java tests/InputTestStateTest.java app/src/main/java/app/m30/companion/FirmwareUpdateResult.java tests/FirmwareUpdateResultTest.java app/src/main/java/app/m30/companion/OfficialM30Files.java app/src/main/java/app/m30/companion/BootUpdateProtocol.java app/src/main/java/app/m30/companion/M30UpdateEngine.java tests/BootUpdateTest.java app/src/main/java/app/m30/companion/M30Firmware.java tests/M30FirmwareTest.java app/src/main/java/app/m30/companion/RadialGuideGeometry.java tests/RadialGuideTest.java app/src/main/java/app/m30/companion/BootVersionProtocol.java tests/BootVersionTest.java app/src/main/java/app/m30/companion/UsbDeviceIdentity.java tests/UsbDeviceIdentityTest.java app/src/main/java/app/m30/companion/BootModelProtocol.java tests/BootModelTest.java app/src/main/java/app/m30/companion/PadState.java app/src/main/java/app/m30/companion/ModeDetector.java app/src/main/java/app/m30/companion/InputMonitor.java app/src/main/java/app/m30/companion/SourceSelector.java app/src/main/java/app/m30/companion/ModeConfirmation.java app/src/main/java/app/m30/companion/ControllerSession.java app/src/main/java/app/m30/companion/ControllerBank.java app/src/main/java/app/m30/companion/SwipeNavigation.java app/src/main/java/app/m30/companion/ConnectionGuide.java app/src/main/java/app/m30/companion/ButtonGuide.java tests/ConnectionGuideTest.java tests/PadStateTest.java tests/DiagnosticsTest.java tests/ReconnectionTest.java tests/SwipeNavigationTest.java tests/ControllersTest.java tests/ButtonGuideTest.java
java -cp "$out" PadStateTest
java -cp "$out" DiagnosticsTest
java -cp "$out" ReconnectionTest

java -cp "$out" SwipeNavigationTest

java -cp "$out" ConnectionGuideTest
java -cp "$out" ControllersTest
java -cp "$out" ButtonGuideTest

java -cp "$out" BootModelTest

java -cp "$out" UsbDeviceIdentityTest

java -cp "$out" BootVersionTest

java -cp "$out" RadialGuideTest

java -cp "$out" M30FirmwareTest

java -cp "$out" app.m30.companion.BootUpdateTest

java -cp "$out" FirmwareUpdateResultTest

java -cp "$out" InputTestStateTest

java -cp "$out" ControllerTelemetryTest

java -cp "$out" DpadDetectorTest

java -cp "$out" RemoteNavigationTest

java -cp "$out" BluetoothSearchTest

java -cp "$out" BluetoothDeviceListTest

java -cp "$out" BluetoothSlotLinksTest

java -cp "$out" BluetoothDeviceLabelTest

java -cp "$out" ControllerAutoFollowTest
javac -cp "$out" -d "$out" tests/ControllerConnectionRequestTest.java
java -cp "$out" ControllerConnectionRequestTest
javac -d "$out" app/src/main/java/app/m30/companion/BluetoothActionPolicy.java tests/BluetoothActionPolicyTest.java
java -cp "$out" BluetoothActionPolicyTest

javac -d "$out" app/src/main/java/app/m30/companion/BluetoothScanPolicy.java tests/BluetoothScanPolicyTest.java
java -cp "$out" BluetoothScanPolicyTest
