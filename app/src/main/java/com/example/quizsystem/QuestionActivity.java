package com.example.quizsystem;


import static com.example.quizsystem.SplashActivity.categoryList;
import static com.example.quizsystem.SplashActivity.selectedCatIndex;
import static com.example.quizsystem.VictorinsActivity.victorinsIDs;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("ALL")
public class QuestionActivity extends AppCompatActivity {

    private TextView question, qCount, timer;
    private Button option1, option2, option3, option4;
    private List<Question> questionList;
    int questionNum;
    private CountDownTimer countDown;
    private int score;
    private FirebaseFirestore firestore;
    private int victorinNumber;
    private Dialog loadingDialog;
    private int selectedOption = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        question = findViewById(R.id.question);
        qCount = findViewById(R.id.question_num);
        timer = findViewById(R.id.countdown);

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);

        enabledButtons(option1, option2, option3, option4);
        option1.setOnClickListener(view -> {
            selectedOption = 1;

            disabledButtons(option1, option2, option3, option4);

            countDown.cancel();
            checkAnswer(selectedOption, view);
        });

        option2.setOnClickListener(view -> {
            selectedOption = 2;

            disabledButtons(option1, option2, option3, option4);

            countDown.cancel();
            checkAnswer(selectedOption, view);
        });


        option3.setOnClickListener(view -> {
            selectedOption = 3;

            disabledButtons(option1, option2, option3, option4);

            countDown.cancel();
            checkAnswer(selectedOption, view);
        });

        option4.setOnClickListener(view -> {
            selectedOption = 4;

            disabledButtons(option1, option2, option3, option4);

            countDown.cancel();
            checkAnswer(selectedOption, view);
        });

        loadingDialog = new Dialog(QuestionActivity.this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.progress_background);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        loadingDialog.show();

        questionList = new ArrayList<>();

        victorinNumber = getIntent().getIntExtra("VICTORIN_NUMBER", 1);
        firestore = FirebaseFirestore.getInstance();

        getQuestionsList();

        score = 0;

    }

    private void getQuestionsList(){
        questionList.clear();
        firestore.collection("TestSystem").document(categoryList.get(selectedCatIndex).getId())
                .collection(victorinsIDs.get(victorinNumber)).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Map<String, QueryDocumentSnapshot> documentList = new ArrayMap<>();

                    for(QueryDocumentSnapshot document: queryDocumentSnapshots){
                        documentList.put(document.getId(), document);
                    }

                    QueryDocumentSnapshot questionsListDocument = documentList.get("QUESTIONS_LIST");

                    String count = Objects.requireNonNull(questionsListDocument).getString("COUNT");

                    for(int i = 0; i < Integer.parseInt(Objects.requireNonNull(count)); i++){
                        String questionID = questionsListDocument.getString("Q" + String.valueOf(i+1) + "_ID");

                        QueryDocumentSnapshot questionDocument = documentList.get(questionID);

                        questionList.add(new Question(
                                Objects.requireNonNull(questionDocument).getString("QUESTION"),
                                questionDocument.getString("A"),
                                questionDocument.getString("B"),
                                questionDocument.getString("C"),
                                questionDocument.getString("D"),
                                Integer.parseInt(Objects.requireNonNull(questionDocument.getString("ANSWER")))
                        ));
                    }

                    setQuestion();

                    loadingDialog.dismiss();

                }).addOnFailureListener(e -> {
            Toast.makeText(QuestionActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
        });

    }

    private void setQuestion(){
        timer.setText(String.valueOf(15));

        question.setText(questionList.get(0).getQuestion());

        option1.setText(questionList.get(0).getOptionA());
        option2.setText(questionList.get(0).getOptionB());
        option3.setText(questionList.get(0).getOptionC());
        option4.setText(questionList.get(0).getOptionD());

        qCount.setText(String.valueOf(1) + "/" + String.valueOf(questionList.size()));

        startTimer();

        questionNum = 0;

    }

    private void startTimer(){
         countDown = new CountDownTimer(16000, 1000) {
            @Override
            public void onTick(long l) {
                if(l < 15000){
                    timer.setText(String.valueOf(l / 1000));
                }
            }

            @Override
            public void onFinish() {
                disabledButtons(option1, option2, option3, option4);
                changeQuestion();
            }
        };

        countDown.start();

    }

    private void checkAnswer(int selectedOption, View view){
        if(selectedOption == questionList.get(questionNum).getCorrectAnswer()){
            //Правильный ответ
            ((Button)view).setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            score++;
        }else{
            //Неправильный ответ
            ((Button)view).setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            switch (questionList.get(questionNum).getCorrectAnswer()){
                case 1:
                    option1.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                    break;
                case 2:
                    option2.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                    break;
                case 3:
                    option3.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                    break;
                case 4:
                    option4.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                    break;
            }
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeQuestion();
            }
        }, 2000);
    }

    private void changeQuestion(){

        if(questionNum < questionList.size() - 1){

            questionNum++;

            playAnimation(question, 0, 0);
            playAnimation(option1, 0, 1);
            playAnimation(option2, 0, 2);
            playAnimation(option3, 0, 3);
            playAnimation(option4, 0, 4);

            qCount.setText(String.valueOf(questionNum+1) + "/" + String.valueOf(questionList.size()));

            timer.setText(String.valueOf(15));

            startTimer();

//            enabledButtons(option1, option2, option3, option4);
        }else{
            Intent intent = new Intent(QuestionActivity.this, ScoreActivity.class);
            intent.putExtra("ОЧКИ", String.valueOf(score) + "/" + String.valueOf(questionList.size()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            QuestionActivity.this.finish();
        }
    }

    private void playAnimation(View view, final int value, int viewNum){
        view.animate().alpha(value).scaleX(value).scaleY(value)
                .setDuration(500)
                .setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                enabledButtons(option1, option2, option3, option4);

                if(value == 0){
                    switch(viewNum){
                        case 0:
                            ((TextView)view).setText(questionList.get(questionNum).getQuestion());
                            break;
                        case 1:
                            ((Button)view).setText(questionList.get(questionNum).getOptionA());
                            break;
                        case 2:
                            ((Button)view).setText(questionList.get(questionNum).getOptionB());
                            break;
                        case 3:
                            ((Button)view).setText(questionList.get(questionNum).getOptionC());
                            break;
                        case 4:
                            ((Button)view).setText(questionList.get(questionNum).getOptionD());
                            break;
                    }

                    if(viewNum != 0){
                        ((Button)view).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#87CEEB")));
                    }

                    playAnimation(view, 1, viewNum);

                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void enabledButtons(Button op1, Button op2, Button op3, Button op4){
        op1.setEnabled(true);
        op2.setEnabled(true);
        op3.setEnabled(true);
        op4.setEnabled(true);
    }

    private void disabledButtons(Button op1, Button op2, Button op3, Button op4){
        op1.setEnabled(false);
        op2.setEnabled(false);
        op3.setEnabled(false);
        op4.setEnabled(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        countDown.cancel();
    }
}