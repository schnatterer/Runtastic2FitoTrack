package info.schnatterer.runtastic2fitotrack;

// Extracted from export PDF
public enum RuntasticSportType {
    RUNNING(1),
    NORDIC_WALKING(2),
    CYCLING(3),
    MOUNTAIN_BIKING(4),
    OTHER(5),
    INLINE_SKATING(6),
    HIKING(7),
    CROSS_COUNTRY_SKIING(8),
    SKIING(9),
    SNOWBOARDING(10),
    MOTORBIKE(11),
    SNOWSHOEING(13),
    TREADMILL(14),
    ERGOMETER(15),
    ELLIPTICAL(16),
    ROWING(17),
    SWIMMING(18),
    WALKING(19),
    RIDING(20),
    GOLFING(21),
    RACE_CYCLING(22),
    TENNIS(23),
    BADMINTON(24),
    SQUASH(25),
    YOGA(26),
    AEROBICS(27),
    MARTIAL_ARTS(28),
    SAILING(29),
    WINDSURFING(30),
    PILATES(31),
    ROCK_CLIMBING(32),
    FRISBEE(33),
    STRENGTH_TRAINING(34),
    VOLLEYBALL(35),
    HANDBIKE(36),
    CROSS_SKATING(37),
    SOCCER(38),
    SURFING(42),
    KITESURFING(43),
    KAYAKING(44),
    BASKETBALL(45),
    SPINNING(46),
    PARAGLIDING(47),
    WAKEBOARDING(48),
    DIVING(50),
    TABLE_TENNIS(51),
    HANDBALL(52),
    BACK_COUNTRY_SKIING(53),
    ICE_SKATING(54),
    SLEDDING(55),
    CURLING(58),
    BIATHLON(60),
    KITE_SKIING(61),
    SPEED_SKIING(62),
    PUSH_UPS(63),
    SIT_UPS(64),
    PULL_UPS(65),
    SQUATS(66),
    AMERICAN_FOOTBALL(67),
    BASEBALL(68),
    CROSSFIT(69),
    DANCING(70),
    ICE_HOCKEY(71),
    SKATEBOARDING(72),
    ZUMBA(73),
    GYMNASTICS(74),
    RUGBY(75),
    STANDUP_PADDLING(76),
    SIXPACK(77),
    BUTT_TRAINING(78),
    LEG_TRAINING(80),
    RESULTS_WORKOUT(81),
    TRAIL_RUNNING(82),
    PLOGGING(84),
    WHEELCHAIR(85),
    E_BIKING(86),
    SCOOTERING(87),
    ROWING_MACHINE(88),
    STAIR_CLIMBING(89),
    JUMPING_ROPE(90),
    TRAMPOLINE(91),
    BODYWEIGHT_TRAINING(92),
    TABATA(93),
    CALLISTHENICS(94),
    SUSPENSION_TRAINING(95),
    POWERLIFTING(96),
    OLYMPIC_WEIGHTLIFTING(97),
    STRETCHING(98),
    MEDITATION(99),
    BOULDERING(100),
    VIA_FERRATA(101),
    PADEL(102),
    POLE_DANCING(103),
    BOXING(104),
    CRICKET(105),
    FIELD_HOCKEY(106),
    TRACK_FIELD(107),
    FENCING(108),
    SKYDIVING(109),
    CHEERLEADING(111),
    E_SPORTS(111),
    LACROSSE(113),
    BEACH_VOLLEYBALL(114),
    VIRTUAL_RUNNING(114),
    VIRTUAL_CYCLING(116);

    private final int id;

    RuntasticSportType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static RuntasticSportType fromId(int id) {
        for (RuntasticSportType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid sport type id: " + id);
    }

    public static final String WORKOUT_TYPE_ID_OTHER = "other";
    public static final String WORKOUT_TYPE_ID_RUNNING = "running";
    public static final String WORKOUT_TYPE_ID_WALKING = "walking";
    public static final String WORKOUT_TYPE_ID_HIKING = "hiking";
    public static final String WORKOUT_TYPE_ID_CYCLING = "cycling";
    public static final String WORKOUT_TYPE_ID_INLINE_SKATING = "inline_skating";
    public static final String WORKOUT_TYPE_ID_SKATEBOARDING = "skateboarding";
    public static final String WORKOUT_TYPE_ID_ROWING = "rowing";
    public static final String WORKOUT_TYPE_ID_SWIMMING = "swimming";
    public static final String WORKOUT_TYPE_ID_TREADMILL = "treadmill";
    public static final String WORKOUT_TYPE_ID_ROPE_SKIPPING = "rope_skipping";
    public static final String WORKOUT_TYPE_ID_TRAMPOLINE_JUMPING = "trampoline_jumping";
    public static final String WORKOUT_TYPE_ID_PUSH_UPS = "push-ups";
    public static final String WORKOUT_TYPE_ID_PULL_UPS = "pull-ups";

    public String toFitoTrack() {
        return switch (this) {
            case RUNNING -> WORKOUT_TYPE_ID_RUNNING;
            case NORDIC_WALKING, WALKING -> WORKOUT_TYPE_ID_WALKING;
            case HIKING -> WORKOUT_TYPE_ID_HIKING;
            case CYCLING, RACE_CYCLING, E_BIKING -> WORKOUT_TYPE_ID_CYCLING;
            case INLINE_SKATING -> WORKOUT_TYPE_ID_INLINE_SKATING;
            case SKATEBOARDING -> WORKOUT_TYPE_ID_SKATEBOARDING;
            case ROWING, ROWING_MACHINE -> WORKOUT_TYPE_ID_ROWING;
            case SWIMMING -> WORKOUT_TYPE_ID_SWIMMING;
            case TREADMILL -> WORKOUT_TYPE_ID_TREADMILL;
            case JUMPING_ROPE -> WORKOUT_TYPE_ID_ROPE_SKIPPING;
            case TRAMPOLINE -> WORKOUT_TYPE_ID_TRAMPOLINE_JUMPING;
            case PUSH_UPS -> WORKOUT_TYPE_ID_PUSH_UPS;
            case PULL_UPS -> WORKOUT_TYPE_ID_PULL_UPS;
            default -> WORKOUT_TYPE_ID_OTHER;
        };
    }
}