import SwiftUI
import MapKit

struct ContentView: View {
    @StateObject private var locationManager = LocationManager()
    @StateObject private var game = GameViewModel()

    var body: some View {
        ZStack(alignment: .top) {
            Map(coordinateRegion: $game.region)
                .ignoresSafeArea()

            VStack(spacing: 8) {
                Text("Snail Game")
                    .font(.title2)
                    .bold()

                Text(statusText)
                    .font(.subheadline)

                HStack(spacing: 12) {
                    Text("Score: \(game.state.score)")
                    if let player = game.state.player {
                        Text("Player: \(format(player.latitude)), \(format(player.longitude))")
                    }
                }
                .font(.caption)

                Button(game.state.isActive ? "Pause" : "Start") {
                    if game.state.isActive {
                        game.pause()
                    } else {
                        game.start()
                    }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding()
        }
        .onAppear {
            locationManager.requestPermission()
            locationManager.start()
            game.start()
        }
        .onReceive(locationManager.$currentCoordinate) { coordinate in
            game.updatePlayer(coordinate)
        }
    }

    private var statusText: String {
        if game.state.snail == nil {
            return "Waiting for location..."
        }
        return game.state.isActive ? "Snail chase active" : "Paused"
    }

    private func format(_ value: Double) -> String {
        String(format: "%.5f", value)
    }
}

#Preview {
    ContentView()
}