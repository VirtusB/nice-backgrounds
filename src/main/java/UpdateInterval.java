public class UpdateInterval {
    private String _displayName;
    private int _intervalSeconds;

    public UpdateInterval(String DisplayName, int IntervalSeconds) {
        this._displayName = DisplayName;
        this._intervalSeconds = IntervalSeconds;
    }

    @Override
    public String toString() {
        return this.get_displayName();
    }

    public String get_displayName() {
        return _displayName;
    }

    public void set_displayName(String _displayName) {
        this._displayName = _displayName;
    }

    public int get_intervalSeconds() {
        return _intervalSeconds;
    }

    public void set_intervalSeconds(int _intervalSeconds) {
        this._intervalSeconds = _intervalSeconds;
    }
}
