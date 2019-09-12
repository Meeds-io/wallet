<script>
export default {
  extends: VueChart.Line,
  props: {
    transactionStatistics: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data () {
    return {
      incomeGradient: null,
      outcomeGradient: null,
    }
  },
  watch: {
    transactionStatistics(oldVal, newVal) {
      this.initializeChart();
    }
  },
  methods: {
    initializeChart() {
      if (!this.transactionStatistics) {
        return;
      }
      this.incomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450)
      this.outcomeGradient = this.$refs.canvas.getContext('2d').createLinearGradient(0, 0, 0, 450)
  
      this.outcomeGradient.addColorStop(0, 'rgba(255, 0,0, 0.5)')
      this.outcomeGradient.addColorStop(0.5, 'rgba(255, 0, 0, 0.25)');
      this.outcomeGradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
      
      this.incomeGradient.addColorStop(0, 'rgba(0, 231, 255, 0.9)')
      this.incomeGradient.addColorStop(0.5, 'rgba(0, 231, 255, 0.25)');
      this.incomeGradient.addColorStop(1, 'rgba(0, 231, 255, 0)');

      this.renderChart({
        labels: this.transactionStatistics.labels,
        datasets: [
          {
            label: this.$t('exoplatform.wallet.chart.Income'),
            borderColor: '#05CBE1',
            pointBackgroundColor: 'white',
            pointBorderColor: 'white',
            borderWidth: 1,
            backgroundColor: this.incomeGradient,
            data: this.transactionStatistics.income,
          },{
            label: this.$t('exoplatform.wallet.chart.Outcome'),
            borderColor: '#FC2525',
            pointBackgroundColor: 'white',
            pointBorderColor: 'white',
            borderWidth: 1,
            backgroundColor: this.outcomeGradient,
            data: this.transactionStatistics.outcome,
          }
        ]
      }, {responsive: true, maintainAspectRatio: false});
    }
  }
}

</script>
