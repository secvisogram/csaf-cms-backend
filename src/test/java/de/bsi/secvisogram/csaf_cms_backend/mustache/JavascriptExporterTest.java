package de.bsi.secvisogram.csaf_cms_backend.mustache;

import static de.bsi.secvisogram.csaf_cms_backend.mustache.JavascriptExporter.determineMediaTypeOfLogo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(properties = "csaf.document.templates.companyLogoPath=./src/test/resources/eXXcellent_solutions.png")
@ExtendWith(SpringExtension.class)
class JavascriptExporterTest {

    private static final String json = """
            {
              "document": {
                "category": "generic_csaf",
                "csaf_version": "2.0",
                "publisher": {
                  "category": "coordinator",
                  "name": "exccellent",
                  "namespace": "https://exccellent.de"
                },
                "title": "TestRSc",
                "tracking": {
                  "current_release_date": "2022-01-11T11:00:00.000Z",
                  "id": "exxcellent-2021AB123",
                  "initial_release_date": "2022-01-12T11:00:00.000Z",
                  "revision_history": [
                    {
                      "date": "2022-01-12T11:00:00.000Z",
                      "number": "0.0.1",
                      "summary": "Test rsvSummary"
                    }
                  ],
                  "status": "draft",
                  "version": "0.0.1",
                  "generator": {
                    "date": "2022-01-11T04:07:27.246Z",
                    "engine": {
                      "version": "1.10.0",
                      "name": "Secvisogram"
                    }
                  }
                },
                "acknowledgments": [
                  {
                    "names": [
                      "Rainer",
                      "Gregor",
                      "Timo"
                    ],
                    "organization": "exxcellent contribute",
                    "summary": "Summary 1234",
                    "urls": [
                      "https://exccellent.de",
                      "https:/heise.de"
                    ]
                  }
                ]
              }
            }
            """;

    private static final String resultHtml =
            """
<!DOCTYPE html>
<html lang="en">

<head>
  <link rel="stylesheet" href="https://unpkg.com/gutenberg-css" charset="utf-8">
  <link rel="stylesheet" href="https://unpkg.com/gutenberg-css/dist/themes/modern.min.css" charset="utf-8">
  <meta charset="utf-8" />
  <style>
    @page {
        size: A4;
    }

    table {
      text-align: left;
    }

    h1 {
      font-size: 2rem;
    }

    h2 {
      font-size: 1.7rem;
    }

    h3 {
      font-size: 1.5rem;
    }

    h4 {
      font-size: 1.35rem;
    }

    h5 {
      font-size: 1.2rem;
    }

    h6 {
      font-size: 1.1rem;
    }

    .logo {
        display: inline-block;
        max-height: 40mm;
        margin-bottom: 20mm;
    }

    .tlp {
        display: table;
        background-color: #000000 !important;
        margin: 0 auto 30px auto;
        padding: 0 3px;
    }

    .RED {
        color: #ff0033;
    }

    .AMBER {
        color: #ffc000;
    }

    .GREEN {
        color: #33ff00;
    }

    .WHITE {
        color: #ffffff;
    }
  </style>
</head>

<body>

  <img class="logo" src="data:image&#x2F;png;base64, iVBORw0KGgoAAAANSUhEUgAAASwAAABGCAYAAAB2Zan9AAAXsUlEQVR4nO2dB7hVxbXHF&#x2F;Y2oMGCvRuNiiVqTARbGEueRlFjeYKYgEadxBo7xBpUjL5IHBtoVCwhYsGOgwk8yRMrIohiJ5ZYUMFRQbG8b938N9nZzux29jn3nHvn933n43L23rNn5uy9ZmatNWtRIBAIBAKBQCAQCAQ6K13CLx&#x2F;oiAipViWiHzia9rQ1+h9ZTRZSLUNEuzkOvWKNnhYemvZhsc7Y6DSEVGsR0daxU&#x2F;5hjX46fomQaisiWjujqJes0c8VuO+iRLRXjkFknDV6XpE2dVJYWN3paPrPiej6HF2ysuf6y4jo+M7eue1FEFjfZlci+lPs2xuI6PDEWZ&#x2F;hnOVTyvlQSPV9a&#x2F;TrOe97MRGdkHHOldbou3KWFwg0HUKqR&#x2F;ifRL2etEYPylPXRcJPWhxr9Ewi+hkRfZVy8XeI6A4h1dJZNxBS9c8hrB4momPr3LRAoN5sTkRbJD4b5L1nEFglsUaPzyFAeOl4ddoJQqptieiajHJeZAFpjf6yEW0LBJqVILBqwBp9BS&#x2F;TMkroL6T6teuAkGoVnoUR0VIp188hop9aoz9qjzYGAs1EEFi1w8LIZJRyqZCqV&#x2F;wLIdUSRDSGiNZIue4rzKxmNkE7A4F2JwisGrFGs1A5kIjShAobN8YIqVaLfcfWpl4p1zDHYukZCHR6qIiVEMpjNvdvCJMv+6l8TkS8VHmViJ6xRr&#x2F;XGXvVGj1HSLU3ET1GRCt4TuPl3+1Cqp1gdTwqo9grseSsBCEV&#x2F;9Y9iWgTIuoRs9TwkvNtIppBRC&#x2F;UW08Gob0dEa1LRN1QD0tE&#x2F;Oy8DIvRh&#x2F;WsQzMgpFqWiFh&#x2F;uRERsc&#x2F;YckTE7iqfsK8XEU2zRr&#x2F;YyKoKqboS0TZEtDEs4IsT0Vx27SGiKdbo19q761IFFh5ytoYNgLl&#x2F;8Yzz+aH&#x2F;MyuaO5vwska&#x2F;JKQ6gP2kUvp1eyJit4Q+GcVVYhEUUrFP1x7wPfoJES2bcclcIdVDcNl40Br9Ta11QD1WhIA+FC9D1vlPEtEoIrrOGv1JFXVoBvA+7Y&#x2F;f48c53j8eSG4nosvLCi8h1cFEdLDj0JHROyqk6gMrtUx7x4VUz0Bne117GYC8TopCKn7QLyei9UuUO5+ILiKiodboL2qrYmMRUh2e9MOyRif9sLwIqY7KoYhPgx&#x2F;M7WtVsgupdiCi4Qkn2CJMISJljX60hjqwM+xviGhIDmHp4gMiOtUafW2Je+&#x2F;rcxy1Rmc6jgqp1iEi14ziMmt0YcdRIRUP+Dxj&#x2F;m7Ra4mIB46RRHSyNXpuwfueTURnOQ6tC3&#x2F;CEWzUKVifyTAEvZ9x7+0xgYmzlkPu8ErtnYx7HmyNnvwtHRaPykIqFjYPlBRWBKsXd9JEIdVKJctoSazRV0HQl6FmiyB+P35I&#x2F;7cGYUVwyZgkpDq5ZD26YaZ4YUlhxXTnF1VIdQuMFC2JkOq3RDS+pLAivOBHENFTQqrcPksZ8Ez3iRLCirBSGCekSrNuE+TA2omPa5K0pOO85KftXi6l+x+J6JQSjXDBDfsrHt7OxPE5LIdJarYIYgl4BQaLKgwqXMYwvHBF6tEVAnOnCurAHMIjNdrXUgipziOicyrat8sTiAlCqqxtYXm4A7OdsvCAdlqjf4v&#x2F;eKiFVMfwMsBzLk&#x2F;PLyWiPaF4XwnTyp0h4J7xXLcZr3mrr3rzktNymOTXFVgET89Q5i&#x2F;gAQQvEJ83kIjOwNLps5TrzhFS7VmgHtdDwe9iGvQl3yeiFWGkWJ2IdiGi38KA46JvhQNpQxBSHUREgz334qXdJdAbrY5+WBmC4Ej8Ti743NugD6uF5A6M6VgZHAcd2y+J6PdENCvlHsfn2clRJQsbLaRaExV0cSs3wBptE8dmE9HrWPrxtbzF5CpHZ+wnpOKlzt2uwoVUp2E2lgbrklw6CSdCqv2I6LCM0yZboy+sR8fGLIePZ+w5ZLQ1uha9V7Qh+zzP4W+gzzrfGj3bc31XCIRTPcpgLaTaEMI4rR4HQLgkYWF5EhTISWV+ZKmcAHXEmRBeSc4WUt2aJ9pCeyOkWh6zXRd3Q5fmsoa+j8F&#x2F;BJTht0Kwx2Hr4tFYDdXKBCI60Ro9xfN7non7HOk43A0GpHsa1d3xB3OwQ9AwtxBRvyyLEY7fKKTiDr&#x2F;PMQU+Bz+UCx6ReXa3ZsotJCvx8oT2EFJtSUQ3edoT8UYO14Ja6Z5Rh4gevNyp0Sp3iWcZOA9LzfvSLrZGf8zPgJBqIhGNddR7Xeg7vIOGkIrvP9Rz+ChrdOZMG0aas4RU&#x2F;O&#x2F;5icNLYRZ5dFY5TcDJ2E+ahHXD+2UJfvTFeCjrJ8ONKM4ZQqprrNGf19DUi2HU8D53&#x2F;HsIqXjV1RsuMUm28wksa&#x2F;SEpBwQUs2BoIsz0Rq9c54KL0L&#x2F;VpD2dxzn0fiYIi+SNfoBWDSSbCmk2s5zDVsI9slYlvAPdqeQyufnRGjLSnAdSBMUfJ99cN+6gDA1d0GhmAWbus8tWw8h1RZYUrkYmCWs4lijTcpGbNfMKU4fqAuSjMsjrBIMxfIxST&#x2F;EqmpaYB11RR&#x2F;g525QHmEVgQH6IsehHiUV5hE80z0lz7sNF4arPIfLGuZKEY3Iu3le8D8WNaOCyzzf7++7AFPSfhnlcufchJH8W2Bd&#x2F;5ccsar6+abAVSCkWg6zlFUKFDcYPjNlONBzDQuKW0uUNxKj72WJz9SM6&#x2F;bxfO+bdXnBi+Ra8iyXw4+tvekNfVSSUdbot0vUjXVLLr+nfWtoZ1FXkYme78tagEsRLQl91px58Gcpw1zH1M83C2iDdVRYM&#x2F;8u5TR2gDzbo+P4HxgB0jiziC6sKBCmN&#x2F;OMssTl1wmpXrZGP1nwOl+bLy7TBswAyii4ezu+46XdBkKq9UqU51pSEZ5Xn3qhGdjBU4cv4edXBhZ0Sate6vtUMT7H1YZ6AEQCy+d9PKzi+23G0+WMKfEFRLQpEf13yjlD2Bs6rsQXUrFl41cZ978F5deToTVM1XmWe7eQapuCI7FrGWbhWtAQ4HLg8jNaosRonsVWjWpXSVy6HkqxwJdlVd5F4DOkVAlHuRWy6uoXJ1paFVm61MLSnqnyQrAUGIh9eWmMElLxPix+WX6QssaOeAz6nEq2m7gQUg2AlS2N4Zh9+uB9ZWMLmotdzrkc2nlB1W1MoRuEUyNYp4HtKkOPBt6rzMy1LGXUQ5USCaxkyNJ6kiqw6F9Caz7W52+knMZm+LvY1A7LVdrLwuXsi3LrAsLHjMgo+zY4lWa5W2yD5WFeZ0OXEG503PdGKsKbffdEI0OPN2qy0RREAmtOAyvTNc9JOS2Hm8CStGrKOY2wCK4Dz+G0zeG8MfwXPMPDUjZpsk9ycIrTYRLX79e9cENq49MG3mu5Bt6rDI0cLFp2y1IZopHgn4ixHOdLCIKqd2Xn3n3PljwhVT8IAx9ZbgP1tgiyAL43Y9RnH6e+icgDZ2EmtUfKdecKqabnMBK85Qhrs7aQSjicfesCW5OFVJ84hAk7zu7eiDo0ET794&#x2F;fwrlVJ2oDe4YgE1gzHi8PHNrZGT2rPRue0HPo4vc4WwUXhibxpxqn9k+FBrNFfC6nYsPAUnDJ9sBtHrwyh+wK2QMVZBNbDhnkhE9FzjlyAPBB+YY3uTC&#x2F;WdM&#x2F;3W1qjn29wXToU0ZLwEU+jsnQtjeICWPiKcFO9tt3EGAY3izTO921JQlSGvhlLiGWghE&#x2F;TVfgGldKe&#x2F;EKqvwmpXnd80pS8Lqskz4APKluPFmWCp9rN8j61LNEMaxzM4Enl+8+FVH+wRs8o0kBk3XUt1T4pY4JlvY+QaiAcR13ZfJM86vE0rgwhFe+tOjGjvAfhM5bWtqkoa1TKaWvCy38Xz1YM9qj&#x2F;g+P7n&#x2F;DWDmu0byOtEyzDXb5dz1ujfZuTmdHYkpKE9wDeViQYHwwOPgfg2c0c2A+qjFccXuB7CKkkdhPkRki1sseoMc8a&#x2F;W79W1R3chts2mZYyCTsSjW1GHaG57bKCKl+hB3erzk+Pk&#x2F;oTGDhOyLn6UfUuMcqFXiyb+DwBI9&#x2F;2In10Jx7xm6Cj05aeawL+pHn+lkYdFyMFlJ9r0Db+qSkHfNtjo&#x2F;qwcvb&#x2F;3McYofH6wvGtBrieYZezTCyNAu+3R43FolpJaTqiTa7+mJgC&#x2F;RDEldAz03ybreKm18vQFiJpHcxP+yThVRs4fK550cjIk95tcdaNjNjFpEKHva8EQ2u5BevXtFOMbpXGuqkgvjtQ7DFKukKwTv9HxVSceTPP&#x2F;lC2wqpesCHjLMALeo4ZWrO3+83HqG1P2KjHZk2Y2dDAZxvfU7A13I46hz1aG+uRl8mnXp74H3iAeovaX6B2Kp1pWf7y9sVRWtoNB84DFQ8AbhISHWsrz8ih&#x2F;OFAssa&#x2F;QGWJmMc56+H0B+TYbF7HB32NZYrP4RnelLxG7EA4TRqESAjUrY8JOkNwZl3RtbyWKOfEFJdihAuSbpi1nSekGoclOPvIezNGrBW9k4J+sez1cPzOKJySGWEiHE50PLvN11IZRC1YCrqsSQMD7siWJ9vg&#x2F;srrRITC1EO+kG&#x2F;mBzAuyN0MFuBb0Pkz1kI4tgDao+DUt6nr+Ei0xALcMXM8uys4QFqRyHVHZAt3TDYboSdDTewauE&#x2F;HNys0ZzV5VTP7nBCzKqsuFVJuHMH1Bgb&#x2F;LQSCstBQqrnrNEu3U5H5VT8wHt72rdKiX6Mfj9fgEYXZ2IZeIjjWBfMBHcrWI&#x2F;ZrZZQ1hr9uJCqPwxGrsFgI&#x2F;RVUX5ljfapAJodk+Lm0jMl8GMb3+pEa&#x2F;QwBOuqQgfEzoQHlowY0IaQap8yu&#x2F;3BJQUjZbY00JcdUOHevU8Qu2l0kYtQj&#x2F;6IUFsFrE7oVdT40wyg7&#x2F;YioipSl83H4FFTsMd25oZaHNWdSwBr9AiEsH24hrbdzz44PGsrWwCiaN6cEQ87bbsNt+9WIVVmaqmOAi9FrNGDYOB4pYZmPYjfb2yZi1loWaNPQjqrzKCLHubDdWTrVs5+jRhxPfEsl93Lyu&#x2F;iVtboGyuuXkOBl8ChGe+tF++eJ2s06zn6IFXPL&#x2F;ACZO0DfA9hPzgS4hO1dASUwGMz4u1Mhx&#x2F;TpJQ9VbwWvoc3SHeGBJ0R7PslpLoXkSP6QT+UGvwQswB2kRhZyxI+UY+&#x2F;IsDg7ph17ZmjHlOhSx1Zw5aqf+L5SZI3vPJnnutLCV9r9FsIPng2XG72zZFF5104&#x2F;l5Xw+&#x2F;xgqcdZTYy3+d4H31Osl6s0fdzRBKE9P6vnNuL2oxFhTJ5CKnWx&#x2F;69teA7sQh+2DfhLf9SFdEQkD5oQobPFXc4h2F5GRuP&#x2F;5ax6ZSP75aVALLWvITNCrzyN8RLshp+v8Vjv99M+Fl9Xc8mwJq8PvQ3PWAQ+ArCkuvxbCvpqWoBCWY3g+FjeTy&#x2F;HyNH3wsZPm8dAliFt8Iz0Q1hsOfh&#x2F;f4QOSM48kjbMrLp0ibhgb7Fk602zt7W6Htj1x2b4vsSwenfj8m4f4cUWIFAR6CK3HVVMziHsDonLqzoX9PM4Ug8kcbRSGUWCARakKYSWEKqn+VIxnB&#x2F;yjm&#x2F;zBF3fDgykQQCgRajaQQWlHA3ZJz2CsLFOPUsiAjA+QjTdCCsyxmDwH+BQKCFaAqBJaRaDdbFrNRc+2UpZKGoPBQOjz5WgOUwK8FpIBBoIhoZyjWNT30be2N8Zo1+L09h7PeCvIBpEUDJE5xwTCI8SNNGBQgEAoFAIBAIBAKBQCAQCAQCgUAgEAgEStF0W3MCgXqDDf1RlqgHrdGTW6HThVRHIzIwISNUK4eZKUWzuDUEAo1ke+SFJMRmaqjAElJxEt1e+O8g3sCf89IlsUGYcuTj7JAEgRUINB6O0LAT7loki&#x2F;WM2G6QlgtmWAVBYAUCLYI1+iEieqgz&#x2F;15BYAXqipBqaQTv2xZJBQiB6Tgg3cNFE5MIqdZAeRtgeTQHQeoesEa&#x2F;X3VbhFSrx7K8vMYp+R3nRGnfmA+t0XmDBBatC9djdfz3rbztxXV7IJbd8og19Sr0d2&#x2F;kLCPeDy9z5igh1ZbIH7AaIohyTLU78u5IoX&#x2F;nXOS6bY6MXfMRZJEzdD2WjK8XBFagbgip+iLdlS+v5ftCqiHW6Kuz6iCkWgEx4gd4jEULhFSshD7DGv1phW3ixLDH4e++iMiaZBsEiCQs2Zzx04RU9+PlXjf2NeeNjGf+vtgafbOnLoci3yVzgid5bvx+yyAfwjGebWqcoJhDLp+YIxpvvB8OFFINQLTQJMOEVJyP856MunVBarrTEbTPxXSk&#x2F;or6tinjYQU6AEioOyYmrHjUfQTp7KOXg49dJaQagQfYCWYwEyEIovOikThKPcYvJAdxHCekytpD2l5wjs8tEGU1YiN8F31yJy1OQ0i1GJaPx8WE1QL0WRRPvQsGgIno47xcHxNWHyMcdYRAHgVfyPIIjvd&#x2F;TkxYvQqhPxkRRwm6vvHI+t5GEFiBenFi7PniWcHq1ugdrdE7ITfAYdj0TshTt2JKPS7EkoGQs47j1Atr9NpQWvePCcEdPDkRm4GHEF89Hqd+Ar6LPrUkDolzSiyP54eI678c+kwgpnwkaDZLSe3nYhkkeF3XGt3NGr0awhxHS9Rlkb0pjZNjxw6xRq9vjd7VGv1DPB+&#x2F;wzEWqr2iAS0sCQP1Yu1Yuc&#x2F;FY+kjDdgoIRWnW1&#x2F;JGn2nrw5Cqm6xlOxchoyn+4IO7CYh1ZuxZdlxQqoLcJ+mwRp9JNp0F5K6MCcUzPmYCeL3nxA7b39r9MIIJPgtxgqpXkLAS5YDA4VUg3PG0z&#x2F;fGj0k&#x2F;gW3QUg1HIklCLNJJ5j9xRPaPJ0oiyOkDBZSvYBnZ0p0LAisQL2YCt0OM1JIxWnbp0BBzsrZGdboSTnu3Su2bHjAl5uQX0gh1ZO454pIqzXFdW4noGdsxvpUXFgl+mwG9Go&#x2F;hV9Xb8Sly+Ipz&#x2F;F4KjZvtisWmEKq52EEYKYJqR5DRqIXORkKkpF8K+R5EFiBesH6iT6xmVako1mIkOpt6EMuskZ&#x2F;7KnHmrG&#x2F;s3yPZsSE5DqdWGDF++z5jHNnQGARsvfUQhErrSKie7G8XALCsnf8BCHVs1h6XhNFGQ46rEBdgLm8J6xAviUP6z7OYGW8kMqXmy6+rMsaYOPPcxWZy5O0ygC&#x2F;IPZ31ju+aOzv1BR4VQLL36bIdPW6p+iekcCKvggzrEDdwKyJFeYXCqm6Qq&#x2F;xXeRH3B7JXbvgweRY&#x2F;H921CXuJ7RtRl3jx9+uQ7uyEgk3C&#x2F;G2p+X2pESfvdnI+lujWVAdzx8kTt4E&#x2F;mybYifAljiV9Wvnsn9bEFiBugC&#x2F;Kc5y&#x2F;DEraCG8Jsf37bE7A7IgM+t56jEJZnjWY+0opNol7pcTK+ugWCZldqF4tqJ2fRD7eyvPOT+uofy0zOZlmQZLJAuB9blvrNGjk2UJqXYkop3x38&#x2F;hctIQkATmUmT3HosM3+9EhhNYBadBeBF814LAClSPkKo7dCNtMxIhFeuxLoEi9Rt8tzVb&#x2F;GI3f81VEXhUXwudB8G6dRoR3W6Nfhce2IdBOEZcXmEG63hq+v5CqvHszQ3fps0xQ8jKo5lkfuz&#x2F;JwmpnuOmQoe0lDV6Zr5i3HDbhVRXxNLh3Sik4gFhlDX6TfhI7Y&#x2F;Zb8T1sM7VHQjK8ejD3YVUF0Jwte0QgJVzr9juAYqWjUGHFagca&#x2F;QHSIgbbavoDz3WHHZlEFLNhqUpUsi&#x2F;6PEgj2A92BP4m32INI&#x2F;GQqpvsIwZCsUt83BBn6IsHoSzJcGSxsvWL+BDNhnCanbBMh+P&#x2F;d0Xaem+xEt5dEX1ZiFg8PcS6KM30GfvoA8Fjj+Z8IuqN+xAPAr3WBwe77OEVO&#x2F;C1WUunocoIsVoa&#x2F;QsCgIrUC+s0SNgJYxb6rrCetcd&#x2F;&#x2F;8a3vC8zJvnq4o12mLpMiwli9FHePD3LLo&#x2F;MQ1r9HyM9q86TuOXf3SJGRZvRfp7VXV0YY1eAG&#x2F;0wSl5Orkvf8&#x2F;6IvRxQ8AsexCEc9xLfmU8H9EymQeFi+GN30YI4BeoO0KqjaHc7YHckx9hZjSp6IZlbCHpBX3VCtAxsen+EWt0LsugkGrJWA7MeXmuw3afXbEMXAaKbcMjP5Yw0WzlCyT0zSpvkZhiuRuWhLwp&#x2F;HFr9ItV1Tt2XS8YPb6DDeMz0f+5loHYxB7NeD6FQEyes1gsXE6RftgW&#x2F;boiJlH8fHCMsL&#x2F;nKSMQCAQCgUAgEAgEAh0fIvp&#x2F;v+QWIBtZmGQAAAAASUVORK5CYII&#x3D;" alt="logo"/>

  <h1>exxcellent-2021AB123: TestRSc</h1>

  <table>
    <tr>
      <td>Publisher: exccellent</td>
      <td>Document category: generic_csaf</td>
    </tr>
    <tr>
      <td>Initial release date: 2022-01-12T11:00:00.000Z</td>
      <td>Engine: Secvisogram 1.10.0</td>
    </tr>
    <tr>
      <td>Current release date: 2022-01-11T11:00:00.000Z</td>
      <td>Build Date: 2022-01-11T04:07:27.246Z</td>
    </tr>
    <tr>
      <td>Current version: 0.0.1</td>
      <td>Status: draft</td>
    </tr>
    <tr>
      <td>CVSSv3.1 Base Score: 0</td>
      <td>Severity:
        
         
      </td>
    </tr>
    <tr>
      <td>Original language: </td>
      <td>Language: </td>
    </tr>
    <tr>
      <td colspan="2">Also referred to: </td>
    </tr>
  </table>



  <h2>Vulnerabilities</h2>

  <h2>Acknowledgments</h2>
  exccellent thanks the following parties for their efforts:
  <ul>

      <li>Rainer, Gregor, Timo from exxcellent contribute  for Summary 1234 (see: 
  <a >https:&#x2F;&#x2F;exccellent.de</a>
, 
  <a >https:&#x2F;heise.de</a>
)</li>
  </ul>


    <h2>exccellent</h2>
    <p>Namespace: https:&#x2F;&#x2F;exccellent.de</p>
    <p></p>
    <p></p>


  <h2>Revision history</h2>
  <table>
    <thead>
      <tr>
        <th>Version</th>
        <th>Date of the revision</th>
        <th>Summary of the revision</th>
      </tr>
    </thead>
    <tbody>
        <tr>
          <td>0.0.1</td>
          <td>2022-01-12T11:00:00.000Z</td>
          <td>Test rsvSummary</td>
        </tr>
    </tbody>
  </table>



</body>

</html>
                    """;

    @Autowired
    private JavascriptExporter javascriptExporter;


    @Test
    void createHtml() throws IOException {
        String html = this.javascriptExporter.createHtml(json);
        assertThat(html, equalToCompressingWhiteSpace(resultHtml));
    }

    @Test
    void determineMediaTypeOfLogoTest() {

      assertThat(determineMediaTypeOfLogo(Path.of("test.png")), is(equalTo(MediaType.IMAGE_PNG)));
      assertThat(determineMediaTypeOfLogo(Path.of("test.jpg")), is(equalTo(MediaType.IMAGE_JPEG)));
      assertThat(determineMediaTypeOfLogo(Path.of("test.jpeg")), is(equalTo(MediaType.IMAGE_JPEG)));
      assertThrows(IllegalArgumentException.class, () -> determineMediaTypeOfLogo(Path.of("test.txt")));
    }
}