import XCTest
@testable import SnailGameIOS

final class GeoMathTests: XCTestCase {
    func testDistanceBetweenNearbyCoordinates() {
        let a = Coordinate(latitude: 40.7128, longitude: -74.0060)
        let b = Coordinate(latitude: 40.7138, longitude: -74.0060)
        XCTAssertEqual(GeoMath.distanceMeters(from: a, to: b), 111.2, accuracy: 1.0)
    }

    func testBearingNorthAndEast() {
        let origin = Coordinate(latitude: 0, longitude: 0)
        XCTAssertEqual(GeoMath.bearingDegrees(from: origin, to: Coordinate(latitude: 1, longitude: 0)), 0, accuracy: 0.1)
        XCTAssertEqual(GeoMath.bearingDegrees(from: origin, to: Coordinate(latitude: 0, longitude: 1)), 90, accuracy: 0.1)
    }

    func testMoveTowardStopsAtTargetWhenStepIsLarge() {
        let start = Coordinate(latitude: 0, longitude: 0)
        let target = Coordinate(latitude: 0, longitude: 0.001)
        XCTAssertEqual(GeoMath.moveToward(from: start, to: target, distanceMeters: 1_000), target)
    }

    func testMoveTowardAdvancesPartWay() {
        let start = Coordinate(latitude: 0, longitude: 0)
        let target = Coordinate(latitude: 0, longitude: 0.001)
        let moved = GeoMath.moveToward(from: start, to: target, distanceMeters: 50)
        XCTAssertGreaterThan(moved.longitude, start.longitude)
        XCTAssertLessThan(moved.longitude, target.longitude)
        XCTAssertEqual(GeoMath.distanceMeters(from: start, to: moved), 50, accuracy: 0.5)
    }

    func testProximityCheck() {
        let player = Coordinate(latitude: 0, longitude: 0)
        let snail = GeoMath.moveToward(from: Coordinate(latitude: 0, longitude: 0.001), to: player, distanceMeters: 106)
        XCTAssertTrue(GeoMath.isWithinProximity(player, snail, radiusMeters: 6))
        XCTAssertFalse(GeoMath.isWithinProximity(player, Coordinate(latitude: 0, longitude: 0.001), radiusMeters: 6))
    }
}
