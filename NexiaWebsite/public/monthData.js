var firebaseConfig = {
    apiKey: "AIzaSyBGjWAAHiBorlpqLcpqxJVly43xET32pNY",
    authDomain: "nexia-df1d0.firebaseapp.com",
    databaseURL: "https://nexia-df1d0.firebaseio.com",
    projectId: "nexia-df1d0",
    storageBucket: "nexia-df1d0.appspot.com",
    messagingSenderId: "464865206070",
    appId: "1:464865206070:web:4dbabf29780d02d5cadd97",
    measurementId: "G-GNVQNZC3SQ"
};
// Initialize Firebase
firebase.initializeApp(firebaseConfig);
firebase.analytics();

var db = firebase.firestore();

function getDateForGraph() {
    var url = new URL(window.location.href);

    var dateParam = url.searchParams.get("date");
    if (dateParam) {
        var split = dateParam.split("-");
        var year = parseInt(split[0]);
        var month = parseInt(split[1]);
        var day = parseInt(split[2]);

        return year + '_' + month + '_' + day;
    } else {
        var date = new Date();
        var year = date.getFullYear();
        var month = date.getMonth() + 1;
        var day = date.getDate();

        return year + '_' + month + '_' + day;
    }
}
async function getData() {
    var split = getDateForGraph().split("_");
    var beginning = split[0] + "_" + split[1] + "_";



    var result = [["Time", "Avg Comp Percentage", "Avg Outdoor Temp"]];

    for (var i = 0; i < 31; i++) {
        var docName = beginning + i;
        var docRef = db.collection("data").doc(docName);

        try {
            var snap = await docRef.get();

            if (snap.exists) {
                var date = new Date();
                date.setFullYear(parseInt(split[0]));
                date.setMonth(parseInt(split[1]) - 1);
                date.setHours(12);
                date.setMinutes(0);
                date.setSeconds(0);
                date.setMinutes(0);
                date.setDate(i);
                var point = [date];

                if (snap.get('avgCompSpeed') != null) {
                    point.push(snap.data().avgCompSpeed * 100);
                } else {
                    continue;
                }

                if (snap.get('avgTemp') != null) {
                    point.push(snap.data().avgTemp);
                } else {
                    continue;
                }
                console.log(point);
                result.push(point);

            }
        } catch (err) {
            console.log(err);
        }
    }

    if (result.length == 1) {
        return null;
    }

    return result;
}

function getMonthFromInt(month) {
    var months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

    return months[month - 1];
}

function drawChart() {
    dataPromise.then(function (dataArray) {
        var data = google.visualization.arrayToDataTable(dataArray);
        var split = getDateForGraph().split("_");
        var date = getMonthFromInt(parseInt(split[1])) + " " + split[0];

        document.getElementById("chart-title").innerText = 'Compressor Usage Over ' + date;

        var options = {
            legend: { position: 'bottom' },
            hAxis: { format: 'dd' },
            vAxis: {
                minValue: 0,
            },
        };

        var chart = new google.visualization.LineChart(document.getElementById('curve_chart'));

        chart.draw(data, options);
    });
}

function loadPage() {
    dataPromise.then(function (dataArray) {
        var loadDiv = document.getElementById("loading_curve");

        if (dataArray) {
            loadDiv.style.display = "none";
            google.charts.load('current', { 'packages': ['corechart'] });
            google.charts.setOnLoadCallback(drawChart);
        } else {
            loadDiv.innerText = "Error: No data for specified month"
        }
    });


}

var dataPromise = getData();
loadPage();



