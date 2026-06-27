import Foundation

enum GeoMath {
    static let earthRadiusMeters = 6_371_000.0

    static func distanceMeters(from: Coordinate, to: Coordinate) -> Double {
        let lat1 = degreesToRadians(from.latitude)
        let lat2 = degreesToRadians(to.latitude)
        let dLat = degreesToRadians(to.latitude - from.latitude)
        let dLon = degreesToRadians(to.longitude - from.longitude)
        let a = pow(sin(dLat / 2), 2) + cos(lat1) * cos(lat2) * pow(sin(dLon / 2), 2)
        return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    static func bearingDegrees(from: Coordinate, to: Coordinate) -> Double {
        let lat1 = degreesToRadians(from.latitude)
        let lat2 = degreesToRadians(to.latitude)
        let dLon = degreesToRadians(to.longitude - from.longitude)
        let y = sin(dLon) * cos(lat2)
        let x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (radiansToDegrees(atan2(y, x)) + 360).truncatingRemainder(dividingBy: 360)
    }

    static func moveToward(from: Coordinate, to: Coordinate, distanceMeters: Double) -> Coordinate {
        let totalDistance = self.distanceMeters(from: from, to: to)
        guard totalDistance > 0.001, distanceMeters < totalDistance else { return to }
        let fraction = distanceMeters / totalDistance
        let phi1 = degreesToRadians(from.latitude)
        let lambda1 = degreesToRadians(from.longitude)
        let phi2 = degreesToRadians(to.latitude)
        let lambda2 = degreesToRadians(to.longitude)
        let angularDistance = totalDistance / earthRadiusMeters
        let sinAngularDistance = sin(angularDistance)
        let a = sin((1 - fraction) * angularDistance) / sinAngularDistance
        let b = sin(fraction * angularDistance) / sinAngularDistance
        let x = a * cos(phi1) * cos(lambda1) + b * cos(phi2) * cos(lambda2)
        let y = a * cos(phi1) * sin(lambda1) + b * cos(phi2) * sin(lambda2)
        let z = a * sin(phi1) + b * sin(phi2)
        return Coordinate(latitude: radiansToDegrees(atan2(z, sqrt(x * x + y * y))), longitude: radiansToDegrees(atan2(y, x)))
    }

    static func isWithinProximity(_ a: Coordinate, _ b: Coordinate, radiusMeters: Double) -> Bool {
        distanceMeters(from: a, to: b) <= radiusMeters
    }

    private static func degreesToRadians(_ degrees: Double) -> Double { degrees * .pi / 180 }
    private static func radiansToDegrees(_ radians: Double) -> Double { radians * 180 / .pi }
}
