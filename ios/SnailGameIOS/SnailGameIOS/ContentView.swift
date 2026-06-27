import CoreLocation
import MapKit
import SwiftUI

struct ContentView: View {
    @StateObject private var locationManager = LocationManager()
    @StateObject private var game = GameViewModel()

    var body: some View {
        ZStack(alignment: .top) {
            Map(position: $game.cameraPosition) {
                UserAnnotation()
                if let snail = game.state.snail {
                    Annotation("Snail", coordinate: snail.clLocationCoordinate) {
                        Image("Snail")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 44, height: 44)
                            .shadow(radius: 4)
                    }
                }
            }
            .mapControls {
                MapUserLocationButton()
                MapCompass()
                MapScaleView()
            }
            .ignoresSafeArea()

            VStack(spacing: 12) {
                statusPanel
                controls
                if let error = locationManager.lastErrorMessage {
                    Text(error).font(.footnote).foregroundStyle(.red).padding(8).background(.thinMaterial).clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding()
        }
        .onAppear {
            locationManager.requestPermission()
            locationManager.start()
        }
        .onReceive(locationManager.$currentCoordinate) { coordinate in
            game.updatePlayer(coordinate)
        }
    }

    private var statusPanel: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(game.state.isActive ? "Snail is following…" : "Snail chase paused")
                .font(.headline)
            Text("Score: \(game.state.score) • Time: \(game.state.elapsedSeconds)s")
            Text("Distance: \(distanceText)")
            Text(permissionText)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var controls: some View {
        HStack {
            Button(game.state.isActive ? "Pause" : "Start") {
                game.state.isActive ? game.stopGame() : game.startGame()
            }
            .buttonStyle(.borderedProminent)

            Button("Reset") { game.resetGame() }
                .buttonStyle(.bordered)
        }
    }

    private var distanceText: String {
        guard let meters = game.state.distanceToSnailMeters else { return "waiting for location" }
        return String(format: "%.1f m", meters)
    }

    private var permissionText: String {
        switch locationManager.authorizationStatus {
        case .notDetermined: return "Location permission not decided."
        case .restricted, .denied: return "Location is disabled for this app. Enable it in Settings."
        case .authorizedAlways, .authorizedWhenInUse: return "Location enabled."
        @unknown default: return "Unknown location permission state."
        }
    }
}

#Preview {
    ContentView()
}
