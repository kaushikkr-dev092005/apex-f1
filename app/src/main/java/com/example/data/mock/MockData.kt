package com.example.data.mock

import com.example.data.database.NewsEntity
import com.example.data.model.Circuit
import com.example.data.model.DrsZone
import com.example.data.model.Constructor
import com.example.data.model.Driver

object MockData {

    val constructors = listOf(
        Constructor("redbull", "Oracle Red Bull Racing", 512, 1, "#3671C6", 28.0, "Milton Keynes, UK"),
        Constructor("mclaren", "McLaren Formula 1 Team", 480, 2, "#FF8000", 26.5, "Woking, UK"),
        Constructor("ferrari", "Scuderia Ferrari", 450, 3, "#E10600", 27.0, "Maranello, Italy"),
        Constructor("mercedes", "Mercedes-AMG PETRONAS F1 Team", 320, 4, "#27F4D2", 23.5, "Brackley, UK"),
        Constructor("astonmartin", "Aston Martin Aramco F1 Team", 94, 5, "#229971", 16.0, "Silverstone, UK"),
        Constructor("rb", "Visa Cash App RB F1 Team", 44, 6, "#6600FF", 11.5, "Faenza, Italy"),
        Constructor("haas", "MoneyGram Haas F1 Team", 38, 7, "#E60000", 10.0, "Kannapolis, USA"),
        Constructor("alpine", "BWT Alpine F1 Team", 21, 8, "#FF80B0", 9.5, "Enstone, UK"),
        Constructor("williams", "Williams Racing", 17, 9, "#00A0DE", 9.0, "Grove, UK"),
        Constructor("kicksauber", "Stake F1 Team Kick Sauber", 4, 10, "#52FF00", 8.0, "Hinwil, Switzerland")
    )

    val drivers = listOf(
        Driver(
            id = "verstappen",
            name = "Max Verstappen",
            number = 1,
            code = "VER",
            team = "Red Bull Racing",
            teamColor = "#3671C6",
            points = 393,
            position = 1,
            wins = 9,
            podiums = 14,
            championships = 3,
            country = "Netherlands",
            age = 28,
            cost = 32.5,
            bio = "Max Verstappen entered F1 as the youngest ever driver. Renowned for his fierce competitive drive, unmatched wet-weather mastery, and aggressive overtaking style, he clinched Red Bull's dominant championships."
        ),
        Driver(
            id = "norris",
            name = "Lando Norris",
            number = 4,
            code = "NOR",
            team = "McLaren",
            teamColor = "#FF8000",
            points = 331,
            position = 2,
            wins = 3,
            podiums = 12,
            championships = 0,
            country = "United Kingdom",
            age = 26,
            cost = 27.0,
            bio = "Lando Norris is McLaren's star driver. Balancing a charismatic, fan-favorite stream personality off-track with surgical speed and high precision execution on-track, he is F1's newest title contender."
        ),
        Driver(
            id = "leclerc",
            name = "Charles Leclerc",
            number = 16,
            code = "LEC",
            team = "Ferrari",
            teamColor = "#E10600",
            points = 307,
            position = 3,
            wins = 2,
            podiums = 11,
            championships = 0,
            country = "Monaco",
            age = 28,
            cost = 25.5,
            bio = "Charles Leclerc is the local hero of Monaco and the 'Il Predestinato' of Ferrari. Famously fast over a single lap, Leclerc holds multiple pole positions and is Ferrari's championship spearhead."
        ),
        Driver(
            id = "piastri",
            name = "Oscar Piastri",
            number = 81,
            code = "PIA",
            team = "McLaren",
            teamColor = "#FF8000",
            points = 262,
            position = 4,
            wins = 2,
            podiums = 7,
            championships = 0,
            country = "Australia",
            age = 25,
            cost = 22.0,
            bio = "Oscar Piastri is one of the most highly rated talents of his generation. Calm, calculated, and ice-cold under pressure, he secured his maiden GP win in his sophomore F1 season with McLaren."
        ),
        Driver(
            id = "sainz",
            name = "Carlos Sainz",
            number = 55,
            code = "SAI",
            team = "Ferrari",
            teamColor = "#E10600",
            points = 244,
            position = 5,
            wins = 2,
            podiums = 8,
            championships = 0,
            country = "Spain",
            age = 31,
            cost = 21.5,
            bio = "The 'Smooth Operator', Carlos Sainz is known for his immense tactical brain and mechanical understanding. He remains the only non-Red Bull driver to win a Grand Prix in 2023."
        ),
        Driver(
            id = "hamilton",
            name = "Lewis Hamilton",
            number = 44,
            code = "HAM",
            team = "Mercedes",
            teamColor = "#27F4D2",
            points = 190,
            position = 6,
            wins = 2,
            podiums = 5,
            championships = 7,
            country = "United Kingdom",
            age = 41,
            cost = 24.0,
            bio = "Lewis Hamilton is statistically the greatest driver in Formula 1 history. Possessing 7 World Titles, over 100 wins, and 100 pole positions, Hamilton combines blistering speed with massive cultural impact."
        ),
        Driver(
            id = "russell",
            name = "George Russell",
            number = 63,
            code = "RUS",
            team = "Mercedes",
            teamColor = "#27F4D2",
            points = 180,
            position = 7,
            wins = 1,
            podiums = 4,
            championships = 0,
            country = "United Kingdom",
            age = 28,
            cost = 20.0,
            bio = "George Russell is Mercedes' future leader. Known as 'Mr. Consistency', Russell pairs analytical perfection with daring, aggressive racing craft and outstanding qualifying speed."
        ),
        Driver(
            id = "alonso",
            name = "Fernando Alonso",
            number = 14,
            code = "ALO",
            team = "Aston Martin",
            teamColor = "#229971",
            points = 62,
            position = 8,
            wins = 0,
            podiums = 2,
            championships = 2,
            country = "Spain",
            age = 44,
            cost = 15.5,
            bio = "Fernando Alonso is the ultimate gladiator of Formula 1. A two-time champion with a career spanning over two decades, Alonso's racecraft, telemetry-reading, and drive are legendary."
        ),
        Driver(
            id = "perez",
            name = "Sergio Perez",
            number = 11,
            code = "PER",
            team = "Red Bull Racing",
            teamColor = "#3671C6",
            points = 151,
            position = 9,
            wins = 0,
            podiums = 4,
            championships = 0,
            country = "Mexico",
            age = 36,
            cost = 18.0,
            bio = "Sergio 'Checo' Perez is the master of tire management and a street circuit specialist. He is crucial to Red Bull's team dynamic, supporting Verstappen's title charges."
        ),
        Driver(
            id = "hulkenberg",
            name = "Nico Hulkenberg",
            number = 27,
            code = "HUL",
            team = "Haas",
            teamColor = "#E60000",
            points = 31,
            position = 10,
            wins = 0,
            podiums = 0,
            championships = 0,
            country = "Germany",
            age = 38,
            cost = 10.5,
            bio = "Nico Hulkenberg is highly respected for his qualifying speed and reliable race execution. A veteran of the sport, he consistently squeezes maximum performance from his Haas machinery."
        )
    )

    val circuits = listOf(
        Circuit(
            id = "monaco",
            name = "Circuit de Monaco",
            country = "Monaco",
            length = "3.337 km",
            laps = 78,
            lapRecord = "1:12.909 (L. Hamilton, 2021)",
            turns = 19,
            drsZones = 1,
            description = "A prestigious and claustrophobic street race around the harbor of Monte Carlo. Overtaking is nearly impossible, making Saturday's qualifying the most intense of the year.",
            trackPoints = listOf(
                Pair(0.15f, 0.65f), Pair(0.20f, 0.50f), Pair(0.30f, 0.40f), Pair(0.40f, 0.35f),
                Pair(0.55f, 0.30f), Pair(0.70f, 0.32f), Pair(0.85f, 0.40f), Pair(0.88f, 0.50f),
                Pair(0.80f, 0.58f), Pair(0.70f, 0.62f), Pair(0.60f, 0.68f), Pair(0.55f, 0.75f),
                Pair(0.48f, 0.85f), Pair(0.38f, 0.88f), Pair(0.28f, 0.82f), Pair(0.20f, 0.75f)
            ),
            drsZonesList = listOf(
                DrsZone(
                    id = "monaco_drs_1",
                    name = "Main Straight DRS",
                    startNodeIndex = 13,
                    endNodeIndex = 15,
                    detectionPoint = "Turn 18 (Anthony Noghès)",
                    description = "Positioned along the start-finish straight, giving a rare chance for close combat leading into Sainte Devote."
                )
            )
        ),
        Circuit(
            id = "silverstone",
            name = "Silverstone Circuit",
            country = "United Kingdom",
            length = "5.891 km",
            laps = 52,
            lapRecord = "1:27.097 (M. Verstappen, 2020)",
            turns = 18,
            drsZones = 2,
            description = "The birthplace of Formula 1. Silverstone features legendary high-speed complexes like Copse, Maggots, and Becketts, pushing drivers' necks and tire grip to their absolute limits.",
            trackPoints = listOf(
                Pair(0.10f, 0.50f), Pair(0.15f, 0.30f), Pair(0.35f, 0.20f), Pair(0.55f, 0.22f),
                Pair(0.75f, 0.30f), Pair(0.85f, 0.45f), Pair(0.90f, 0.60f), Pair(0.75f, 0.70f),
                Pair(0.60f, 0.65f), Pair(0.48f, 0.58f), Pair(0.40f, 0.68f), Pair(0.30f, 0.80f),
                Pair(0.18f, 0.75f), Pair(0.12f, 0.62f)
            ),
            drsZonesList = listOf(
                DrsZone(
                    id = "silverstone_drs_1",
                    name = "Wellington Straight DRS",
                    startNodeIndex = 1,
                    endNodeIndex = 3,
                    detectionPoint = "Turn 4 (The Loop)",
                    description = "Runs along Wellington Straight, enabling passing maneuvers going into the slow Brooklands corner."
                ),
                DrsZone(
                    id = "silverstone_drs_2",
                    name = "Hangar Straight DRS",
                    startNodeIndex = 6,
                    endNodeIndex = 8,
                    detectionPoint = "Turn 11 (Becketts)",
                    description = "Launches down the famous Hangar Straight after the blindingly fast Maggots and Becketts complex."
                )
            )
        ),
        Circuit(
            id = "spa",
            name = "Circuit de Spa-Francorchamps",
            country = "Belgium",
            length = "7.004 km",
            laps = 44,
            lapRecord = "1:46.286 (V. Bottas, 2018)",
            turns = 19,
            drsZones = 2,
            description = "Nestled in the beautiful Ardennes forest, Spa is a fan and driver favorite. It features the legendary Eau Rouge-Raidillon hill climb, extreme elevation changes, and highly unpredictable weather.",
            trackPoints = listOf(
                Pair(0.15f, 0.80f), Pair(0.10f, 0.50f), Pair(0.18f, 0.35f), Pair(0.30f, 0.25f),
                Pair(0.45f, 0.20f), Pair(0.60f, 0.28f), Pair(0.75f, 0.40f), Pair(0.85f, 0.55f),
                Pair(0.78f, 0.72f), Pair(0.65f, 0.68f), Pair(0.52f, 0.62f), Pair(0.40f, 0.65f),
                Pair(0.30f, 0.75f), Pair(0.22f, 0.85f)
            ),
            drsZonesList = listOf(
                DrsZone(
                    id = "spa_drs_1",
                    name = "Kemmel Straight DRS",
                    startNodeIndex = 3,
                    endNodeIndex = 5,
                    detectionPoint = "Turn 2 (Eau Rouge)",
                    description = "The prime slipstreaming and overtaking spot on the calendar, immediately after descending and climbing the epic Eau Rouge-Raidillon hill."
                ),
                DrsZone(
                    id = "spa_drs_2",
                    name = "Start-Finish Straight DRS",
                    startNodeIndex = 12,
                    endNodeIndex = 13,
                    detectionPoint = "Turn 19 (Bus Stop)",
                    description = "A short DRS zone starting out of the final Bus Stop chicane to setup a pass into the tight La Source hairpin."
                )
            )
        ),
        Circuit(
            id = "monza",
            name = "Autodromo Nazionale Monza",
            country = "Italy",
            length = "5.793 km",
            laps = 53,
            lapRecord = "1:21.046 (R. Barrichello, 2004)",
            turns = 11,
            drsZones = 2,
            description = "The 'Temple of Speed'. Monza is characterized by long straights and fast corners, demanding ultra-low-downforce aerodynamic setups. Home of Ferrari's passionate 'Tifosi'.",
            trackPoints = listOf(
                Pair(0.10f, 0.30f), Pair(0.40f, 0.28f), Pair(0.70f, 0.26f), Pair(0.90f, 0.35f),
                Pair(0.88f, 0.50f), Pair(0.75f, 0.65f), Pair(0.55f, 0.75f), Pair(0.35f, 0.78f),
                Pair(0.15f, 0.72f), Pair(0.08f, 0.55f)
            ),
            drsZonesList = listOf(
                DrsZone(
                    id = "monza_drs_1",
                    name = "Rettifilo Straight DRS",
                    startNodeIndex = 8,
                    endNodeIndex = 9,
                    detectionPoint = "Turn 11 (Parabolica)",
                    description = "Massive slipstream zone down the main pit straight, ending at the heavy braking zone of the Rettifilo Chicane."
                ),
                DrsZone(
                    id = "monza_drs_2",
                    name = "Serraglio Straight DRS",
                    startNodeIndex = 4,
                    endNodeIndex = 6,
                    detectionPoint = "Turn 7 (Lesmo 2)",
                    description = "Runs along the back straight under the old banking bridge, leading into the fast Variante Ascari."
                )
            )
        )
    )

    val initialNews = listOf(
        NewsEntity(
            id = "news_1",
            title = "Ferrari Upgrades Package Unveiled for Italian Grand Prix",
            summary = "Scuderia Ferrari introduces a highly anticipated custom low-downforce wing and floor upgrades aiming for Monza victory.",
            content = "MARANELLO — Scuderia Ferrari has officially confirmed an aggressive aerodynamic upgrade package specifically tailored for their home race at Monza. The upgrades feature an extremely low-drag rear wing design and modified underfloor tunnels to maximize straight-line speed. Principal Fred Vasseur noted that the simulator tests look promising, but managing tire degradation through the Curva Grande will remain their biggest challenge during Sunday's main race. Fans are expecting a fierce podium battle with McLaren and Red Bull.",
            category = "TECH UPDATE",
            imageUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=600&auto=format&fit=crop", // placeholder standard sporty car image
            publishedAt = "2026-06-29T10:30:00Z",
            likesCount = 412
        ),
        NewsEntity(
            id = "news_2",
            title = "Lando Norris Clinches Sensational Pole in Rain-Soaked Silverstone",
            summary = "McLaren's Norris masterfully tames a drying British track to pip Max Verstappen by a microscopic 0.032 seconds.",
            content = "SILVERSTONE — In one of the most exciting qualifying sessions of the decade, Lando Norris took pole position for the British Grand Prix in front of an ecstatic home crowd. The session started under heavy rain, leading to multiple yellow flags, but a late dry line emerged. Norris switched to soft slick tires at the absolute last second, timing his final flying lap to perfection. 'It was the scariest lap of my life, but it paid off,' Norris said over the radio, holding off Max Verstappen and Lewis Hamilton.",
            category = "RACE REPORT",
            imageUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?q=80&w=600&auto=format&fit=crop",
            publishedAt = "2026-06-28T16:45:00Z",
            likesCount = 892
        ),
        NewsEntity(
            id = "news_3",
            title = "Fantasy League Alert: Driver Value Recalibration",
            summary = "Oscar Piastri's budget cost rises after strong podium runs, while Hamilton's price dips, opening interesting budget strategies.",
            content = "LONDON — The official F1 Fantasy League regulators have completed their weekly value updates. Oscar Piastri is the week's biggest climber, with his price rising to $22.0M after consecutive podiums. Meanwhile, Lewis Hamilton becomes an attractive buy at $24.0M after a tricky weekend in Spain. Analysts suggest doubling up on McLaren drivers due to their team consistency, or investing in the high-downforce Red Bull constructor package for the upcoming high-speed races.",
            category = "FANTASY",
            imageUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?q=80&w=600&auto=format&fit=crop",
            publishedAt = "2026-06-27T08:15:00Z",
            likesCount = 215
        )
    )
}
