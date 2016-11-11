package imu.recommender.models.user;

public class Personality {

	private double Q1;
	private double Q2;
	private double Q3;
	private double Q4;
	private double Q5;
	private double Q6;
	private double Q7;
	private double Q8;
	private double Q9;
	private double Q10;
	private double type;
	private String typeStr;
	private double Extraversion;
	private double Openness;
	private double Neuroticism;
	private double Agreeableness;
	private double Consientiousness;
	//per personality type scores
	private boolean scores_calculated = false;

	public Personality() {
		Q1 = 1.0;
		Q2 = 1.0;
		Q3 = 1.0;
		Q4 = 1.0;
		Q5 = 1.0;
		Q6 = 1.0;
		Q7 = 1.0;
		Q8 = 1.0;
		Q9 = 1.0;
		Q10 = 1.0;
		type = 0.0;
		typeStr = "";
		Extraversion = 0.0;
		Openness = 0.0;
		Neuroticism = 0.0;
		Agreeableness = 0.0;
		Consientiousness = 0.0;

	}

	public double getQ1() {
		return Q1;
	}

	public void setQ1(double q1) {
		Q1 = q1;
	}

	public double getQ2() {
		return Q2;
	}

	public void setQ2(double q2) {
		Q2 = q2;
	}

	public double getQ3() {
		return Q3;
	}

	public void setQ3(double q3) {
		Q3 = q3;
	}

	public double getQ4() {
		return Q4;
	}

	public void setQ4(double q4) {
		Q4 = q4;
	}

	public double getQ5() {
		return Q5;
	}

	public void setQ5(double q5) {
		Q5 = q5;
	}

	public double getQ6() {
		return Q6;
	}

	public void setQ6(double q6) {
		Q6 = q6;
	}

	public double getQ7() {
		return Q7;
	}

	public void setQ7(double q7) {
		Q7 = q7;
	}

	public double getQ8() {
		return Q8;
	}

	public void setQ8(double q8) {
		Q8 = q8;
	}

	public double getQ9() {
		return Q9;
	}

	public void setQ9(double q9) {
		Q9 = q9;
	}

	public double getQ10() {
		return Q10;
	}

	public void setQ10(double q10) {
		Q10 = q10;
	}

	public double getType() {
		return type;
	}

	public void setType(double type) {
		this.type = type;
	}

	public String getTypeStr() {
		return typeStr;
	}

	public void setTypeStr(String typeStr) {
		this.typeStr = typeStr;
	}

	public boolean isScores_calculated() {
		return scores_calculated;
	}

	public void setScores_calculated(boolean scores_calculated) {
		this.scores_calculated = scores_calculated;
	}

	public double getExtraversion() {
		return Extraversion;
	}

	public void setExtraversion(double extraversion) {
		Extraversion = extraversion;
	}
	public double getOpenness(){
		return Openness;
	}
	public void setOpenness(double openness){
		Openness = openness;
	}
	public double getNeuroticism(){
		return Neuroticism;
	}
	public void setNeuroticism(double neuroticism){
		Neuroticism = neuroticism;
	}
	public double getAgreeableness(){
		return Agreeableness;
	}
	public void setAgreeableness(double agreeableness){
		Agreeableness = agreeableness;
	}
	public double getConsientiousness(){
		return Consientiousness;
	}
	public void setConsientiousness(double consientiousness){
		Consientiousness = consientiousness;
	}

}
